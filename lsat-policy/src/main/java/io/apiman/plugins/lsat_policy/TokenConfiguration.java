package io.apiman.plugins.lsat_policy;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class TokenConfiguration implements Serializable {

    private static final long serialVersionUID = 683486516910591478L;

    @JsonProperty
    private String tlsx;
    @JsonProperty
    private String macaroonx;
    @JsonProperty
    private String secretx;
    @JsonProperty
    private String host;
    @JsonProperty
    private Integer port;


    public String getTlsx() {
        return tlsx;
    }

    public void setTlsx(String tlsx) {
        this.tlsx = tlsx;
    }

    public String getMacaroonx() {
        return macaroonx;
    }

    public void setMacaroonx(String macaroonx) {
        this.macaroonx = macaroonx;
    }

    public String getSecretx() {
        return secretx;
    }

    public void setSecretx(String secretx) {
        this.secretx = secretx;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "TokenConfiguration{" +
                "tls='" + tlsx + '\'' +
                ", macaroon='" + macaroonx + '\'' +
                '}';
    }
}
