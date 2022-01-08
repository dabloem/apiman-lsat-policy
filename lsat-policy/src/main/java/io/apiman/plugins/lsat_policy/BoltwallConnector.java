package io.apiman.plugins.lsat_policy;

import com.github.nitram509.jmacaroons.*;
import com.github.nitram509.jmacaroons.verifier.TimestampCaveatVerifier;
import io.apiman.common.util.Preconditions;
import io.apiman.gateway.engine.IApiConnection;
import io.apiman.gateway.engine.IApiConnectionResponse;
import io.apiman.gateway.engine.IApiConnector;
import io.apiman.gateway.engine.async.AsyncResultImpl;
import io.apiman.gateway.engine.async.IAsyncHandler;
import io.apiman.gateway.engine.async.IAsyncResultHandler;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.beans.exceptions.ConfigurationParseException;
import io.apiman.gateway.engine.beans.exceptions.ConnectorException;
import io.apiman.gateway.engine.beans.util.HeaderMap;
import io.apiman.gateway.engine.components.IPolicyFailureFactoryComponent;
import io.apiman.gateway.engine.io.ByteBuffer;
import io.apiman.gateway.engine.io.IApimanBuffer;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import org.json.JSONObject;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.lightningj.lnd.wrapper.message.Invoice;
import org.lightningj.lnd.wrapper.message.SignMessageResponse;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.stream.Stream;

public class BoltwallConnector implements IApiConnector {

    private final ApiRequest request;
    private final TokenConfiguration config;
//    private final Object failureFactory;

    private final HeaderMap requestHeaders;
    final IPolicyContext context;

    private SynchronousLndAPI synchronousLndAPI;

    public BoltwallConnector(ApiRequest request, TokenConfiguration config, IPolicyContext context, IPolicyFailureFactoryComponent failureFactory) {
        this.request = request;
        this.config = config;
        this.context = context;
        this.requestHeaders = request.getHeaders();

        setLndAPI();
    }

    private void setLndAPI() {
        try {
            byte[] cert = config.getTlsx().getBytes();
            SslContext sslContext =
                    GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)
                            .trustManager(new ByteArrayInputStream(cert))
                            .build();
            synchronousLndAPI = new SynchronousLndAPI(config.getHost(), config.getPort(), sslContext, () -> config.getMacaroonx());
        } catch (Exception ex) {
            throw new ConfigurationParseException(ex.getMessage());
        }
    }

    @Override
    public IApiConnection connect(ApiRequest apiRequest, IAsyncResultHandler<IApiConnectionResponse> iAsyncResultHandler) throws ConnectorException {
        return new LightingApiConnection(iAsyncResultHandler);
    }

    class LightingApiConnection implements IApiConnection, IApiConnectionResponse {

        private static final String CHALLENGE_ID = "challenge";

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
            String mac = context.getAttribute("macaroon", (String)null);

            Macaroon macaroon = MacaroonsBuilder.deserialize(mac);
            verifyInvoice(macaroon);
            Macaroon modifiedMacaroon = addVerificationCaveat(macaroon);

            ByteBuffer bodyBuffer = new ByteBuffer(new JSONObject().put("macaroon", modifiedMacaroon.serialize()).toString());

            bodyHandler.handle(bodyBuffer);
            endHandler.handle((Void) null);
            finished = true;
        }

        //Adds an additional caveat
        private Macaroon addVerificationCaveat(Macaroon macaroon) {
            CaveatPacket challenge = Stream.of(macaroon.caveatPackets)
                    .filter(c -> c.getValueAsText().startsWith(CHALLENGE_ID))
                    .findFirst()
                    .orElseThrow(() -> new MacaroonValidationException("challenge missing.", macaroon));

            String[] strings = challenge.getValueAsText().split(":");
            String msg = strings[0].split("=")[1];
            String pubkey = strings[1];
            context.getLogger(BoltwallConnector.class).info(String.format("challenge: msg = %s, pub = %s", msg, pubkey));

            try {
                SignMessageResponse signMessageResponse = synchronousLndAPI.signMessage(msg.getBytes());

                return MacaroonsBuilder.modify(macaroon)
                    .add_first_party_caveat(challenge.getValueAsText() + signMessageResponse.getSignature())
                    .getMacaroon();
            } catch (Exception ex) {
                throw new IllegalArgumentException(ex.getMessage());
            }
        }

        private void verifyInvoice(Macaroon macaroon) {
            try {
                Preconditions.checkArgument(new MacaroonsVerifier(macaroon)
                        .satisfyGeneral(new TimestampCaveatVerifier())
                        .satisfyGeneral(new GeneralCaveatVerifier() {
                                @Override
                                public boolean verifyCaveat(String s) {
                                    return s.startsWith(CHALLENGE_ID);
                                }
                            })
                        .satisfyGeneral(new GlobCaveatVerifier("resource", null)) //pass any, this is to retrieve the additional challenge
                        .isValid(config.getSecretx()), "Verification failed.");

                //After validation, we can now safely obtain the invoiceId/paymentHash to verify payment
                byte[] bytes = macaroon.identifier.getBytes();
                byte[] paymentHash = Arrays.copyOfRange(bytes, 4, 68);
                Preconditions.checkArgument(paymentHash.length == 64, "paymentHash invalid");

                Invoice invoice = synchronousLndAPI.lookupInvoice(new String(paymentHash),  null);
                Preconditions.checkArgument(invoice.getSettled(), "invoice is not paid yet.");
            } catch (Exception ex) {
                throw new IllegalArgumentException(ex.getMessage());
            }
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
