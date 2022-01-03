package io.apiman.plugins.lsat_pricing;

import io.apiman.plugins.lsat.RegExCaveatVerifier;
import org.junit.Assert;
import org.junit.Test;

public class RegExCaveatVerifierTest {

    @Test
    public void verifyCaveat() {
        RegExCaveatVerifier regExCaveatVerifier = new RegExCaveatVerifier("resource", "/BTCUSD?day=10");
        Assert.assertTrue(regExCaveatVerifier.verifyCaveat("resource=/BTCUSD*"));
        Assert.assertTrue(regExCaveatVerifier.verifyCaveat("resource = /BTCUSD*"));

        Assert.assertFalse(regExCaveatVerifier.verifyCaveat("test=/BTCUSD.*"));
        Assert.assertFalse(regExCaveatVerifier.verifyCaveat("resource=/USDBTC.*"));


        regExCaveatVerifier = new RegExCaveatVerifier("resource", "/quote/BTCUSD?day=10");
        Assert.assertTrue(regExCaveatVerifier.verifyCaveat("resource = /*/BTCUSD*"));
        Assert.assertFalse(regExCaveatVerifier.verifyCaveat("resource = /*/BTCUSD"));
    }
}