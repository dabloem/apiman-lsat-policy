package io.apiman.plugins.lsat_policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

public class ChallengeVerifierTest {

    static ChallengeVerifier challengeVerifer;

    @BeforeClass
    public static void init() throws IOException {
        URL resource = ChallengeVerifier.class.getResource("/config.json");
        ObjectMapper mapper = new ObjectMapper();
        LsatConfiguration lsatConfiguration = mapper.readValue(resource, LsatConfiguration.class);
        challengeVerifer = new ChallengeVerifier(lsatConfiguration);
    }

    @Test
    public void verifyCaveat() {
        Assert.assertTrue(challengeVerifer.verifyCaveat("challenge=fFCB7RlnppjGkXXR23hhJE9lVQuBLc/PX7LS9pONIHk=:Y01BFk1n3SzGUGNRtiErgp2zEmfheXleNl8Oerlh88s=:d6oz3wx6177gxracxptezmj81yund81brt38ow1ojf166uhqgcxnsfdkd8bfbqbr6wx7b433y641ukg7387m8tjijuieekbuhxck5zrx"));
    }

    @Test
    public void parse() {

    }
}