package io.apiman.plugins.lsat_pricing;

import com.github.nitram509.jmacaroons.MacaroonsBuilder;
import com.github.nitram509.jmacaroons.MacaroonsVerifier;
import com.github.nitram509.jmacaroons.verifier.TimestampCaveatVerifier;
import io.apiman.plugins.lsat.MacaroonsVerifierBuilder;
import io.apiman.plugins.lsat.RegExCaveatVerifier;
import org.junit.Assert;
import org.junit.Test;

public class MacaroonsVerifierBuilderTest {

    @Test
    public void build() {
        //WHEN
        MacaroonsVerifierBuilder macaroonsVerifierBuilder = new MacaroonsVerifierBuilder()
            .withCaveatVerifier(new TimestampCaveatVerifier())
            .withCaveatVerifier(new RegExCaveatVerifier("resource", "/BTCUSD?price=21"));

        //THEN
        MacaroonsVerifier macaroonsVerifier = macaroonsVerifierBuilder
                .build(MacaroonsBuilder.deserialize("MDAxMmxvY2F0aW9uIGxzYXQKMDA5NGlkZW50aWZpZXIgMDAwMDk4NDkyZGJhNTdkMmJhYzUxMjJhYWNhYmVkN2FiMzQyNTIxNWZiZTJhYTQ2YjE1MzVhYmViZGE1ZmMwNDJjZWQ1MDNkOWE1ZjI3YzJlOGNjMzYyZDY5NjcwZTM0MjQ4ZjA5MTc1NGZjOTdhODJiZWMzNWIxY2JlMjY0NzBhMzk2CjAwMWNjaWQgcmVzb3VyY2UgPSAvQlRDVVNEKgowMDJkY2lkIHRpbWUgPCAyMDIyLTAxLTA0VDE2OjE0OjI5LjEwMyswMTowMAowMDJmc2lnbmF0dXJlIKBzgGWpAXIBAcOSUSwpB5ZjtozMVJAfCMd0CwpbPqQiCg"));

        //ASSERT
        Assert.assertTrue( macaroonsVerifier.isValid("0201036c6e6402f8") );
    }
}