/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.plugins.lsat_policy;

import com.github.nitram509.jmacaroons.Macaroon;
import com.github.nitram509.jmacaroons.MacaroonsBuilder;
import com.github.nitram509.jmacaroons.verifier.TimestampCaveatVerifier;
import com.google.common.base.Preconditions;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.beans.PolicyFailureType;
import io.apiman.gateway.engine.beans.util.HeaderMap;
import io.apiman.gateway.engine.policies.AbstractMappedPolicy;
import io.apiman.gateway.engine.policy.IPolicyChain;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.lsat.MacaroonsVerifierBuilder;
import io.apiman.plugins.lsat.RegExCaveatVerifier;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import org.apache.commons.codec.binary.Hex;
import org.lightningj.lnd.wrapper.StatusException;
import org.lightningj.lnd.wrapper.SynchronousLndAPI;
import org.lightningj.lnd.wrapper.ValidationException;
import org.lightningj.lnd.wrapper.message.AddInvoiceResponse;
import org.lightningj.lnd.wrapper.message.Invoice;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A policy that adds and validates LSAT Authentication.
 *
 * @author Duncan Bloem
 */
public class LsatPolicy extends AbstractMappedPolicy<LsatConfiguration> {

    private static final String PRICE = "satoshis";

    @Override
    protected Class<LsatConfiguration> getConfigurationClass() {
        return LsatConfiguration.class;
    }

    @Override
    protected void doApply(ApiRequest request, IPolicyContext context, LsatConfiguration config, IPolicyChain<ApiRequest> chain) {
        String authorization = request.getHeaders().get("Authorization");
        String price = request.getHeaders().get("X-" + PRICE);
        context.setAttribute(PRICE, price == null ? null : Integer.valueOf(price));

        if (authorization == null) {
            try {
                byte[] cert = config.getTls().getBytes();
                SslContext sslContext =
                        GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)
                                .trustManager(new ByteArrayInputStream(cert))
                                .build();

                SynchronousLndAPI synchronousLndAPI = new SynchronousLndAPI(config.getHostx(), config.getPortx(), sslContext, () -> config.getMacaroon());
                Invoice invoice = new Invoice();
                invoice.setValue(context.getAttribute(PRICE, config.getPrice()));
                AddInvoiceResponse invoiceResponse = synchronousLndAPI.addInvoice(invoice);

                String bolt11 = invoiceResponse.getPaymentRequest();

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
                outputStream.write(new byte[2]);
                outputStream.write(invoiceResponse.getRHash());
                byte[] array = new byte[32];
                new Random().nextBytes(array);
                outputStream.write( array );

                List<String> caveats = context.getAttribute("caveats", Collections.EMPTY_LIST);
                MacaroonsBuilder builder = new MacaroonsBuilder("lsat", config.getSecret(), Hex.encodeHexString(outputStream.toByteArray()));
                for (String caveat : caveats) {
                    System.out.println("adding caveat:" + caveat);
                    builder.add_first_party_caveat(caveat);
                }
                Macaroon macaroon = builder.getMacaroon();
                String serialize = macaroon.serialize();

                chain.doFailure(new PolicyFailure(PolicyFailureType.Authorization, 402, "Payment required.") {
                    @Override
                    public HeaderMap getHeaders() {
                        HeaderMap headerMap = new HeaderMap();
                        headerMap.add("WWW-Authenticate", String.format("LSAT macaroon=\"%s\", invoice=\"%s\"", serialize, bolt11));
                        return headerMap;
                    }

                    @Override
                    public int getResponseCode() {
                        return 402;
                    }
                });
            } catch (IOException | ValidationException | StatusException e) {
                System.out.println(e.getMessage());
                chain.throwError(new Exception(e.getMessage()));
            }
        } else {
            System.out.println("Authorization:" + authorization);
            if (authorization.startsWith("LSAT ")) {
                try {
                    System.out.println(new URL(request.getUrl()).getPath());

                    String token = authorization.substring("LSAT ".length());
                    String[] strings = token.split(":");
                    Preconditions.checkArgument(strings.length == 2, "LSAT invalid operands.");
                    Macaroon macaroon = MacaroonsBuilder.deserialize(strings[0]);

                    MacaroonsVerifierBuilder verifierBuilder = context.getAttribute("verifier", new MacaroonsVerifierBuilder());
                    Preconditions.checkArgument(
                            verifierBuilder.build(macaroon).isValid(config.getSecret()),
                            "Invalid macaroon"
                    );

                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] encodedhash = digest.digest(hexStringToByteArray(strings[1]));
                    Preconditions.checkArgument(
                            macaroon.identifier.substring(4,68).equalsIgnoreCase(DatatypeConverter.printHexBinary(encodedhash)), "Invalid preimage."
                    );

                    chain.doApply(request);
                } catch (Exception e) {
                    PolicyFailure policyFailure = new PolicyFailure(PolicyFailureType.Other, 500, e.getMessage());
                    policyFailure.setResponseCode(e instanceof NoSuchAlgorithmException ? 500 : 400);
                    chain.doFailure(policyFailure);
                }
            } else {
                chain.doFailure(new PolicyFailure(PolicyFailureType.Authentication, 401, "Invalid authentication mechanism. Only LSAT allowed"));
            }
        }
    }

    @Override
    public void doApply(ApiResponse response, IPolicyContext context, LsatConfiguration config, IPolicyChain<ApiResponse> chain) {
        chain.doApply(response);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

}
