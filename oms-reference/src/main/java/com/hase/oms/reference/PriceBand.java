package com.hase.oms.reference;

/** Price band (inclusive), prices scaled by 1e8 to match OCG-C decimals. */
public record PriceBand(long lower, long upper) {

    public PriceBand {
        if (lower > upper) {
            throw new IllegalArgumentException("lower > upper: " + lower + " > " + upper);
        }
    }

    public boolean contains(long price) {
        return price >= lower && price <= upper;
    }
}
