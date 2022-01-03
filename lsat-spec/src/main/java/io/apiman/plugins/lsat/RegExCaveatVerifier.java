package io.apiman.plugins.lsat;

import com.github.nitram509.jmacaroons.GeneralCaveatVerifier;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

public class RegExCaveatVerifier implements GeneralCaveatVerifier {

    private String key;
    private String url;

    public RegExCaveatVerifier(String key, String url) {
        this.url = url;
        this.key = key;
    }

    @Override
    public boolean verifyCaveat(String caveat) {
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
