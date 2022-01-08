package io.apiman.plugins.lsat_policy;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class LsatConfiguration implements Serializable {

    private static final long serialVersionUID = 683486516910591477L;

    @JsonProperty
    private String hostx;
    @JsonProperty
    private Integer portx;
    @JsonProperty
    private String tls;
    @JsonProperty
    private String macaroon;
    @JsonProperty
    private String secret;
    @JsonProperty
    private Integer price;

    public String getHostx() {
        return hostx;
    }

    public void setHostx(String hostx) {
        this.hostx = hostx;
    }

    public Integer getPortx() {
        return portx;
    }

    public void setPortx(Integer portx) {
        this.portx = portx;
    }

    public String getTls() {
        return tls;
    }

    public void setTls(String tls) {
        this.tls = tls;
    }

    public String getMacaroon() {
        return macaroon;
    }

    public void setMacaroon(String macaroon) {
        this.macaroon = macaroon;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "LSATConfiguration{" +
                "tls='" + tls + '\'' +
                ", macaroon='" + macaroon + '\'' +
                ", price=" + price +
                '}';
    }
}
