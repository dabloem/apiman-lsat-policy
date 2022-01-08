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
import com.github.nitram509.jmacaroons.MacaroonsVerifier;
import com.github.nitram509.jmacaroons.verifier.TimestampCaveatVerifier;
import com.google.common.base.Preconditions;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.PolicyFailure;
import io.apiman.gateway.engine.beans.PolicyFailureType;
import io.apiman.gateway.engine.beans.exceptions.ConfigurationParseException;
import io.apiman.gateway.engine.beans.util.HeaderMap;
import io.apiman.gateway.engine.policies.AbstractMappedPolicy;
import io.apiman.gateway.engine.policies.PolicyFailureCodes;
import io.apiman.gateway.engine.policy.IPolicyChain;
import io.apiman.gateway.engine.policy.IPolicyContext;
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

import javax.net.ssl.SSLException;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Base64;
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

    private static SynchronousLndAPI synchronousLndAPI;

    @Override
    public LsatConfiguration parseConfiguration(String jsonConfiguration) throws ConfigurationParseException {
        LsatConfiguration config = super.parseConfiguration(jsonConfiguration);

        try {
            byte[] cert = config.getTls().getBytes();
            SslContext sslContext =
                    GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)
                            .trustManager(new ByteArrayInputStream(cert))
                            .build();

            synchronousLndAPI = new SynchronousLndAPI(config.getHostx(), config.getPortx(), sslContext, () -> config.getMacaroon());
        } catch (Exception ex) {
            throw new ConfigurationParseException(ex);
        }

        return config;
    }

    @Override
    protected Class<LsatConfiguration> getConfigurationClass() {
        return LsatConfiguration.class;
    }

    @Override
    protected void doApply(ApiRequest request, IPolicyContext context, LsatConfiguration config, IPolicyChain<ApiRequest> chain) {
        if (!request.getHeaders().containsKey("Authorization")) {
            //Do Authenticate challenge
            try {
                AddInvoiceResponse invoice = getInvoice(context.getAttribute(PRICE, config.getPrice()), config);

                Macaroon macaroon = createMacaroon(context, config, invoice);

                chain.doFailure(new PolicyFailure(PolicyFailureType.Authorization, 402, "Payment required.") {
                    @Override
                    public HeaderMap getHeaders() {
                        HeaderMap headerMap = new HeaderMap();
                        headerMap.add("WWW-Authenticate",
                                String.format("LSAT macaroon=\"%s\", invoice=\"%s\"", macaroon.serialize(), invoice.getPaymentRequest()));
                        return headerMap;
                    }

                    @Override
                    public int getResponseCode() {
                        return 402;
                    }
                });

            } catch (IOException | ValidationException | StatusException e) {
                chain.throwError(new Exception(e.getMessage()));
            }
        } else {
            //Do Authorize
            LsatToken lsatToken = new LsatToken(request.getHeaders().get("Authorization"));
            if (lsatToken.isNative()) {
                context.getLogger(getClass()).info("LSAT authorization");
                try {
                    Macaroon macaroon = MacaroonsBuilder.deserialize(lsatToken.getMacaroon());

                    //Check macaroon
                    MacaroonsVerifier verifier = getMacaroonsVerifier(request.getUrl(), macaroon);
                    Preconditions.checkArgument(verifier.isValid(config.getSecret()));
                    //Check invoice paid status via pre-image
                    Preconditions.checkArgument(verifyPayment(macaroon, lsatToken.getPreimage()), "Invalid preimage");

//                    context.setAttribute(AuthorizationPolicy.AUTHENTICATED_USER_ROLES, "reader"); //TODO TBD
                    chain.doApply(request);
                } catch (IllegalArgumentException | MalformedURLException e) {
                    PolicyFailure policyFailure = new PolicyFailure(PolicyFailureType.Other, PolicyFailureCodes.USER_NOT_AUTHORIZED, e.getMessage());
                    policyFailure.setResponseCode(400);
                    chain.doFailure(policyFailure);
                }
            } else if (lsatToken.isBoltwall()) { //&& boltwall enabled?
                context.getLogger(getClass()).info("Boltwall authorization");
                try {
                    Macaroon macaroon = MacaroonsBuilder.deserialize(lsatToken.getMacaroon());
                    MacaroonsVerifier verifier = getMacaroonsVerifier(request.getUrl(), macaroon)
                            .satisfyGeneral(new ChallengeVerifier(config)); //Additional verifier for Boltwall

                    Preconditions.checkArgument(verifier.isValid(config.getSecret()));

                    chain.doApply(request);
                } catch (Exception ex) {
                    chain.doFailure(new PolicyFailure(PolicyFailureType.Other, 402, ex.getMessage()));
                }
            } else {
                chain.doFailure(new PolicyFailure(PolicyFailureType.Authentication, 401, "Invalid authentication mechanism. Only LSAT allowed"));
            }
        }
    }

    private MacaroonsVerifier getMacaroonsVerifier(String url, Macaroon macaroon) throws MalformedURLException {
        MacaroonsVerifier verifier = new MacaroonsVerifier(macaroon)
                .satisfyGeneral(new TimestampCaveatVerifier())
                .satisfyGeneral(new GlobCaveatVerifier("resource", new URL(url).getPath())); //TODO make 'resource' static
        return verifier;
    }

    private Macaroon createMacaroon(IPolicyContext context, LsatConfiguration config, AddInvoiceResponse invoice) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(new byte[2]);
        outputStream.write(invoice.getRHash());
        byte[] array = new byte[32];
        new Random().nextBytes(array);
        outputStream.write( array );
        byte[] identifier = outputStream.toByteArray();


        MacaroonsBuilder builder = new MacaroonsBuilder("lsat", config.getSecret(), Hex.encodeHexString(identifier));
        //Boltwall
        byte[] random = new byte[32];
        new Random().nextBytes(random); //random
        Base64.Encoder encoder = Base64.getEncoder();
        builder.add_first_party_caveat("challenge=" + encoder.encodeToString(random) + ":" + encoder.encodeToString(invoice.getPaymentAddr()) + ":");

        List<String> caveats = context.getAttribute("caveats", Collections.EMPTY_LIST);
        for (String caveat : caveats) {
            builder.add_first_party_caveat(caveat);
        }
        return builder.getMacaroon();
    }

    private AddInvoiceResponse getInvoice(Integer price, LsatConfiguration config) throws StatusException, ValidationException, SSLException {
        Invoice invoice = new Invoice();
        invoice.setValue(price);
        return synchronousLndAPI.addInvoice(invoice);
    }

    private boolean verifyPayment(Macaroon macaroon, String preimage) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(hexStringToByteArray(preimage));
            return macaroon.identifier.substring(4, 68).equalsIgnoreCase(DatatypeConverter.printHexBinary(encodedhash));
        } catch (Exception e) {
            return false;
        }
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
