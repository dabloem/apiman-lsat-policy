package io.apiman.plugins.token_policy;

import com.github.nitram509.jmacaroons.GeneralCaveatVerifier;

public class ChallengeVerifier implements GeneralCaveatVerifier {

    private static final String CHALLENGE_ID = "boltwall";

    @Override
    public boolean verifyCaveat(String caveat) {
        if (!caveat.startsWith(CHALLENGE_ID)){
            return false;
        }

        String[] challenge = caveat.split(":");
        String message = challenge[0].split("=")[1];
        String pubkey = challenge[1];



        return false;
    }
}
