package io.apiman.plugins.token_policy;

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
import io.apiman.gateway.engine.components.IBufferFactoryComponent;
import io.apiman.gateway.engine.components.IPolicyFailureFactoryComponent;
import io.apiman.gateway.engine.io.AbstractStream;
import io.apiman.gateway.engine.io.ByteBuffer;
import io.apiman.gateway.engine.io.IApimanBuffer;
import io.apiman.gateway.engine.io.IReadWriteStream;
import io.apiman.gateway.engine.policies.AbstractMappedDataPolicy;
import io.apiman.gateway.engine.policies.AbstractMappedPolicy;
import io.apiman.gateway.engine.policy.IConnectorInterceptor;
import io.apiman.gateway.engine.policy.IPolicyChain;
import io.apiman.gateway.engine.policy.IPolicyContext;
import org.json.JSONObject;

import java.util.Arrays;

public class TokenPolicy extends AbstractMappedDataPolicy<TokenConfig> {

    @Override
    protected Class<TokenConfig> getConfigurationClass() {
        return TokenConfig.class;
    }

    @Override
    protected void doApply(ApiRequest request, IPolicyContext context, TokenConfig config, IPolicyChain<ApiRequest> chain) {
        Preconditions.checkArgument(Arrays.asList("GET", "POST").contains(request.getType()), "Only POST or GET requests allowed.");

        if (request.getType().equals("GET") && request.getHeaders().containsKey("X-macaroon")) {
            context.setAttribute("macaroon", request.getHeaders().get("X-macaroon"));
        }

        context.setConnectorInterceptor(new IConnectorInterceptor() {
            @Override
            public IApiConnector createConnector() {
                return new BoltwallConnector(request, config, context, context.getComponent(IPolicyFailureFactoryComponent.class));
            }
        });

        chain.doSkip(request);
    }

    @Override
    protected IReadWriteStream<ApiResponse> responseDataHandler(ApiResponse apiResponse, IPolicyContext iPolicyContext, TokenConfig tokenConfig) {
        return null;
    }

    @Override
    protected IReadWriteStream<ApiRequest> requestDataHandler(ApiRequest request, IPolicyContext context, TokenConfig config) {
        System.out.println("requestDataHandler()");

        if (!request.getType().equalsIgnoreCase("POST")) {
            //GET request contains macaroon in header
            return null;
        }

        final String CONTENT_LENGTH = "Content-Length";

        final IBufferFactoryComponent bufferFactory = context.getComponent(IBufferFactoryComponent.class);
        final int contentLength = request.getHeaders().containsKey(CONTENT_LENGTH)
                ? Integer.parseInt(request.getHeaders().get(CONTENT_LENGTH))
                : 0;

        return new AbstractStream<ApiRequest>() {

            private IApimanBuffer readBuffer = bufferFactory.createBuffer(contentLength);

            @Override
            public void write(IApimanBuffer chunk) {
                readBuffer.append(chunk.getBytes());
            }

            @Override
            protected void handleHead(ApiRequest apiRequest) {

            }

            @Override
            public ApiRequest getHead() {
                return request;
            }

            @Override
            public void end() {
                if (readBuffer.length() > 0) {
                    String body = new String(readBuffer.getBytes());
                    JSONObject jsonBody = transform(body);
                    context.setAttribute("macaroon", jsonBody.get("macaroon"));
                }

                super.end();
            }

            private JSONObject transform(String json) {
                JSONObject jsonObject = null;
                if (json.trim().startsWith("{")) { //$NON-NLS-1$
                    jsonObject = new JSONObject(json);
                    System.out.println(jsonObject.toString());
                } else {
                    System.out.println("json macaroon not found.");
                }
                return jsonObject;
            }
        };


    }

}
