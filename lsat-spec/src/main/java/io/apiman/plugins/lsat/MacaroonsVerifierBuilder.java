package io.apiman.plugins.lsat;

import com.github.nitram509.jmacaroons.GeneralCaveatVerifier;
import com.github.nitram509.jmacaroons.Macaroon;
import com.github.nitram509.jmacaroons.MacaroonsVerifier;

import java.util.ArrayList;
import java.util.List;

public class MacaroonsVerifierBuilder {

    private List<GeneralCaveatVerifier> caveatVerifiers = new ArrayList<>();

    public MacaroonsVerifierBuilder withCaveatVerifier(GeneralCaveatVerifier caveatVerifier) {
        caveatVerifiers.add(caveatVerifier);
        return this;
    }

    public MacaroonsVerifier build(Macaroon macaroon) {
        MacaroonsVerifier verifier = new MacaroonsVerifier(macaroon);
        for (GeneralCaveatVerifier cv : caveatVerifiers) {
            verifier.satisfyGeneral(cv);
        }

        return verifier;
    }

    @Override
    public String toString() {
        return "MacaroonsVerifierBuilder{" +
                "caveatVerifiers=" + caveatVerifiers +
                '}';
    }
}
