package io.apiman.plugins.lsat_pricing;


import io.apiman.test.common.mock.EchoResponse;
import io.apiman.test.policies.*;
import org.junit.Assert;
import org.junit.Test;

@TestingPolicy(PricingPolicy.class)
public class PricingPolicyTest extends ApimanPolicyTest {

    @Test
    @Configuration(classpathConfigFile = "config.json")
    public void testLSAT() throws Throwable {
        // Send a test HTTP request to the API (resulting in executing the policy).
        try {
            PolicyTestResponse response = send(PolicyTestRequest.build(PolicyTestRequestType.GET, "/some/resource?price=1600"));

            // Now do some assertions on the result!
            EchoResponse entity = response.entity(EchoResponse.class);
            Assert.assertEquals("GET", entity.getMethod());
        } catch (PolicyFailureError e) {
            System.out.println(e.getFailure().getHeaders());
            Assert.fail();
        }
    }

    @Test
//    @Ignore
    @Configuration(classpathConfigFile = "config.json")
    public void testLSATAuthenticatoin() throws Throwable {
        // Send a test HTTP request to the API (resulting in executing the policy).
        try {
            PolicyTestResponse response = send(PolicyTestRequest
                    .build(PolicyTestRequestType.GET, "/some/resource")
                    .header("Authorization", "LSAT MDAxMmxvY2F0aW9uIGxzYXQKMDA5NGlkZW50aWZpZXIgMDAwMDJmNDNhMjJiZGZmNWQ5NmU5ZWY3YzhkZGE0YTUyZmFmZThjOGJkZjJlNjgxMWJkMTg3YzZlYWVmZGM3ODc5MTgyZjYzOTMzZTE4ZjFmNjJjNWM1NWI5ODk4ZWFhZGVmZGFmMWJjZjNmNmExZDU2MThmOWM4OGFiYjk1ZDg1YTkwCjAwMmZzaWduYXR1cmUgY8Wf361lJh4fCnMQsXSW0T0Vp5RWR_WDLZxaLFQrzBEK:9fe6231bb0e21588b7def0117ac3f1be90cba4e08e4b750400c511af11a1bc51")
            );

            // Now do some assertions on the result!
            Assert.assertEquals(200, response.code());
            EchoResponse entity = response.entity(EchoResponse.class);
            Assert.assertEquals("GET", entity.getMethod());
            Assert.assertEquals("/some/resource", entity.getResource());
        } catch (PolicyFailureError e) {
            System.out.println(e.getFailure().getHeaders());
            Assert.fail();
        }
    }

}