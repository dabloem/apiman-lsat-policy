package io.apiman.plugins.token_policy;

import com.github.nitram509.jmacaroons.*;
import io.apiman.common.util.Preconditions;
import io.apiman.gateway.engine.IApiConnection;
import io.apiman.gateway.engine.IApiConnectionResponse;
import io.apiman.gateway.engine.IApiConnector;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.beans.exceptions.ConnectorException;
import io.apiman.gateway.engine.beans.util.HeaderMap;
import io.apiman.gateway.engine.components.IPolicyFailureFactoryComponent;
import io.apiman.gateway.engine.io.ByteBuffer;
import io.apiman.gateway.engine.io.IApimanBuffer;
import io.apiman.gateway.engine.policy.IPolicyContext;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.stream.Stream;

public class BoltwallConnector implements IApiConnector {

//    private final ApiRequest request;
    private final TokenConfig config;
//    private final Object failureFactory;

    private final HeaderMap requestHeaders;
    final IPolicyContext context;

    public BoltwallConnector(ApiRequest request, TokenConfig config, IPolicyContext context, IPolicyFailureFactoryComponent failureFactory) {
//        this.request = request;
        this.config = config;
        this.context = context;
        this.requestHeaders = request.getHeaders();
    }

    @Override
    public IApiConnection connect(ApiRequest apiRequest, IAsyncResultHandler<IApiConnectionResponse> iAsyncResultHandler) throws ConnectorException {
        return new LightingApiConnection(iAsyncResultHandler);
    }

    class LightingApiConnection implements IApiConnection, IApiConnectionResponse {

        private static final String CHALLENGE_ID = "boltwall";

        private boolean finished = false;
        private IAsyncHandler<Void> endHandler;
        private IAsyncResultHandler<IApiConnectionResponse> responseIAsyncResultHandler;

        private ApiResponse response;

        private IAsyncHandler<IApimanBuffer> bodyHandler;


        public LightingApiConnection(IAsyncResultHandler<IApiConnectionResponse> handler) {
            this.responseIAsyncResultHandler = handler;

            response = new ApiResponse();
            response.setCode(200);
            response.getHeaders().add("Content-type", "application/json");
        }

        @Override
        public void transmit() {
            System.out.println("TRANSMIT");
            String mac = context.getAttribute("macaroon", (String)null);

            Macaroon macaroon = MacaroonsBuilder.deserialize(mac);
            verifyInvoice(macaroon);
            Macaroon result = addCaveat(macaroon);

            ByteBuffer bodyBuffer = new ByteBuffer(new JSONObject().put("macaroon", result.serialize()).toString());
            bodyBuffer.append("\n");

            bodyHandler.handle(bodyBuffer);
            endHandler.handle((Void) null);
            finished = true;
        }

        private Macaroon addCaveat(Macaroon macaroon) {
            CaveatPacket challenge = Stream.of(macaroon.caveatPackets)
                    .filter(c -> c.getValueAsText().startsWith(CHALLENGE_ID))
                    .findFirst()
                    .orElseThrow(() -> new MacaroonValidationException("challenge missing.", macaroon));

            String[] strings = challenge.getValueAsText().split(":");
            String msg = strings[0].split("=")[1];
            String pubkey = strings[1];

            System.out.println(String.format("challenge: msg = %s, pub = %s", msg, pubkey));


            return MacaroonsBuilder.modify(macaroon)
                    .add_first_party_caveat(challenge.getValueAsText() + ":invalidsig")
                    .getMacaroon();
        }

        private void verifyInvoice(Macaroon macaroon) {
            //TODO
//            Preconditions.checkArgument(new MacaroonsVerifier(macaroon).isValid("0201036c6e6402f8"), "Verification failed.");

            byte[] bytes = macaroon.identifier.getBytes();
            byte[] paymentHash = Arrays.copyOfRange(bytes, 4, 36);
            System.out.println("payment hash:" + paymentHash);

            //TODO
            //check if payment is paid

            Preconditions.checkArgument(paymentHash.length == 32, "paymentHash invalid");
        }

        private Macaroon createMacaroon(Macaroon macaroon) {
            return MacaroonsBuilder.modify(macaroon)
                    .add_first_party_caveat("challenge=valid")
                    .getMacaroon();
        }

        @Override
        public void bodyHandler(IAsyncHandler<IApimanBuffer> iAsyncHandler) {
            this.bodyHandler = iAsyncHandler;
        }

        @Override
        public void endHandler(IAsyncHandler<Void> iAsyncHandler) {
            this.endHandler = iAsyncHandler;
        }

        @Override
        public ApiResponse getHead() {
            return response;
        }

        @Override
        public void abort(Throwable throwable) {

        }

        @Override
        public void write(IApimanBuffer iApimanBuffer) {

        }

        @Override
        public void end() {
            responseIAsyncResultHandler.handle(AsyncResultImpl.<IApiConnectionResponse> create(this));
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public boolean isConnected() {
            return !finished;
        }
    }
}
