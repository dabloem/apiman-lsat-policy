package io.apiman.plugins.lsat_policy;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;

public class LsatToken {

    private String macaroon;
    private String preimage;

    public LsatToken(String header) {
        try {
            String token = header.substring("LSAT ".length());
            String[] strings = token.split(":");
            Preconditions.checkArgument(strings.length < 3, "LSAT invalid operands.");
            macaroon = strings[0];
            if (strings.length == 2) {
                preimage = strings[1];
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public String getMacaroon() {
        return macaroon;
    }

    public String getPreimage() {
        return preimage;
    }

    public boolean isNative() {
        return StringUtils.isNoneBlank(macaroon, preimage);
    }

    public boolean isBoltwall() {
        return StringUtils.isNoneBlank(macaroon) && StringUtils.isBlank(preimage);
    }
}
