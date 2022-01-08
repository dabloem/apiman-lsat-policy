package io.apiman.plugins.lsat_policy;

import org.junit.Assert;
import org.junit.Test;

public class GlobCaveatVerifierTest {

    @Test
    public void verifyCaveat() {
        GlobCaveatVerifier globCaveatVerifier = new GlobCaveatVerifier("resource", "/BTCUSD?day=10");
        Assert.assertTrue(globCaveatVerifier.verifyCaveat("resource=/BTCUSD*"));
        Assert.assertTrue(globCaveatVerifier.verifyCaveat("resource = /BTCUSD*"));

        Assert.assertFalse(globCaveatVerifier.verifyCaveat("test=/BTCUSD.*"));
        Assert.assertFalse(globCaveatVerifier.verifyCaveat("resource=/USDBTC.*"));


        globCaveatVerifier = new GlobCaveatVerifier("resource", "/quote/BTCUSD?day=10");
        Assert.assertFalse(globCaveatVerifier.verifyCaveat("resource = /*/BTCUSD"));
        Assert.assertTrue(globCaveatVerifier.verifyCaveat("resource = /*/BTCUSD*"));

        globCaveatVerifier = new GlobCaveatVerifier("resource", null);
        Assert.assertTrue(globCaveatVerifier.verifyCaveat("resource = /*/BTCUSD*"));
    }
}