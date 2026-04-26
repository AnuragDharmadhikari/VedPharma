package org.ved.crm.product;

import lombok.Getter;

@Getter
public enum GstRate {

    GST_5(5),
    GST_12(12),
    GST_18(18);

    private final int rate;

    GstRate(int rate) {
        this.rate = rate;
    }

}
