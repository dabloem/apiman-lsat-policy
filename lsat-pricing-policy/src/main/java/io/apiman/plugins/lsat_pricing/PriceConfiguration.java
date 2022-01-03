package io.apiman.plugins.lsat_pricing;

import java.io.Serializable;

public class PriceConfiguration implements Serializable {

    private static final long serialVersionUID = 683486516910591477L;

    private Integer price;

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "LSATConfiguration{" +
                "price=" + price +
                '}';
    }
}
