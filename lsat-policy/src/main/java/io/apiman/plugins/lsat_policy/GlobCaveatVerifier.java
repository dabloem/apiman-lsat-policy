package io.apiman.plugins.lsat_policy;

import com.github.nitram509.jmacaroons.GeneralCaveatVerifier;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

public class GlobCaveatVerifier implements GeneralCaveatVerifier {

    private String key;
    private String url;

    /**
     *
     * @param key the caveat key to take into consideration
     * @param url URL to match against, or null to pass always
     */
    public GlobCaveatVerifier(String key, String url) {
        this.url = url;
        this.key = key;
    }

    @Override
    public boolean verifyCaveat(String caveat) {
        if (StringUtils.isBlank(url)) {
            return true;
        }

        if (caveat.startsWith(key)) {
            String regex = caveat.split("=")[1].trim();
            PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + regex);
            return pathMatcher.matches(Paths.get(url));
        }

        return false;
    }

    @Override
    public String toString() {
        return "RegExCaveatVerifier{" +
                "key='" + key + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
