package io.apiman.plugins.lsat_pricing;


import io.apiman.plugins.lsat_policy.LsatPolicy;
import io.apiman.test.common.mock.EchoResponse;
import io.apiman.test.policies.*;
import org.junit.Assert;
import org.junit.Test;

@TestingPolicy(LsatPolicy.class)
public class LsatPolicyTest extends ApimanPolicyTest {

    @Test
    @Configuration(classpathConfigFile = "config.json")
    public void testLSAT() throws Throwable {
        // Send a test HTTP request to the API (resulting in executing the policy).
        try {
            PolicyTestResponse response = send(PolicyTestRequest.build(PolicyTestRequestType.GET, "/some/resource?price=1600"));

            // Now do some assertions on the result!
            Assert.assertEquals(402, response.code());
            EchoResponse entity = response.entity(EchoResponse.class);
            Assert.assertEquals("GET", entity.getMethod());
            Assert.assertEquals("/some/resource", entity.getResource());
            Assert.assertEquals("LSAT adsadas", entity.getHeaders().get("WWW-Authenticate"));
        } catch (PolicyFailureError e) {
            System.out.println(e.getFailure().getHeaders());
        }
    }

    @Test
//    @Ignore
    @Configuration(classpathConfigFile = "config.json")
    public void testLSATAuthentication() throws Throwable {
        // Send a test HTTP request to the API (resulting in executing the policy).
        try {
            PolicyTestResponse response = send(PolicyTestRequest
                    .build(PolicyTestRequestType.GET, "/some/resource")
                    .header("Authorization", "LSAT MDAxMmxvY2F0aW9uIGxzYXQKMDA5NGlkZW50aWZpZXIgMDAwMDJmNDNhMjJiZGZmNWQ5NmU5ZWY3YzhkZGE0YTUyZmFmZThjOGJkZjJlNjgxMWJkMTg3YzZlYWVmZGM3ODc5MTgyZjYzOTMzZTE4ZjFmNjJjNWM1NWI5ODk4ZWFhZGVmZGFmMWJjZjNmNmExZDU2MThmOWM4OGFiYjk1ZDg1YTkwCjAwMmZzaWduYXR1cmUgY8Wf361lJh4fCnMQsXSW0T0Vp5RWR_WDLZxaLFQrzBEK:9fe6231bb0e21588b7def0117ac3f1be90cba4e08e4b750400c511af11a1bc51")
//                    .header("Authorization", "LSAT MDAxMmxvY2F0aW9uIGxzYXQKMDA5NGlkZW50aWZpZXIgMDAwMDY0Zjc3NTEyNjA1MWZhMDk2ZDQ0YTYwMzZjYTUyMWEyNGY1MjkxN2EyODc2Mzk1MDg1NjdmODgyZjJmYWRmYmZhODRhNGFjMzZhYTNlMGRmYTkwYTM1OWVlNzIxOTlmOTkwNjQ3NzU4YjllNzg0ZWI4NzFkODExMmNkMGE2Mjg5CjAwMzNjaWQgcmVzb3VyY2UgPSAvYXBpbWFuLWdhdGV3YXkvTFNBVC5jb20vdjEvMS4wCjAwMjhjaWQgdGltZSA8IDIwMjItMDEtMDRUMjE6MzA6MDIuNDgyWgowMDJmc2lnbmF0dXJlIDJVLom0VAcINiZBWB2gxLwhb9V4MzAbmmns2zyfSMGHCg:3437fc37670bdee9e481995d0ac1cc26dae86da2c01df9142dd46538e512c28f")
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