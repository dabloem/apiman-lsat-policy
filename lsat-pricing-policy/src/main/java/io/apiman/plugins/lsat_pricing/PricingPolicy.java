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
package io.apiman.plugins.lsat_pricing;

import com.github.nitram509.jmacaroons.verifier.TimestampCaveatVerifier;
import io.apiman.gateway.engine.beans.ApiRequest;
import io.apiman.gateway.engine.beans.ApiResponse;
import io.apiman.gateway.engine.policies.AbstractMappedPolicy;
import io.apiman.gateway.engine.policy.IPolicyChain;
import io.apiman.gateway.engine.policy.IPolicyContext;
import io.apiman.plugins.lsat.MacaroonsVerifierBuilder;
import io.apiman.plugins.lsat.RegExCaveatVerifier;
import org.joda.time.DateTime;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * A policy that adds and validates LSAT Authentication.
 *
 * @author Duncan Bloem
 */
public class PricingPolicy extends AbstractMappedPolicy<PriceConfiguration> {

    private static final String PRICE = "satoshis";

    @Override
    protected Class<PriceConfiguration> getConfigurationClass() {
        return PriceConfiguration.class;
    }

    @Override
    protected void doApply(ApiRequest request, IPolicyContext context, PriceConfiguration config, IPolicyChain<ApiRequest> chain) {
        String authorization = request.getHeaders().get("authorization");
        if (authorization != null && authorization.startsWith("LSAT")) {
            //do add verifierBuilder
            try {
                context.setAttribute("verifier", new MacaroonsVerifierBuilder()
                    .withCaveatVerifier(new TimestampCaveatVerifier())
                    .withCaveatVerifier(new RegExCaveatVerifier("resource", new URL(request.getUrl()).getPath()))
                );
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            System.out.println("context enriched with verifier");
        } else {
            //do add caveats and price
            context.setAttribute("caveats", Arrays.asList("resource = " + "/**", "time < " + DateTime.now().plusDays(1)));
            context.setAttribute(PRICE, 100);
            System.out.println("context enriched with caveats & price");
        }

        chain.doApply(request);
    }

    @Override
    public void doApply(ApiResponse response, IPolicyContext context, PriceConfiguration config, IPolicyChain<ApiResponse> chain) {
        chain.doApply(response);
    }

}
