package com.hase.oms.order;

/**
 * Content-level ingress validation (DESIGN.md §6.4). Invalid input is rejected
 * locally with {@link IllegalArgumentException} before it consumes venue
 * throttle.
 */
public final class IngressValidator {

    private IngressValidator() {}

    public static void validateText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (text.length() > 10) {
            throw new IllegalArgumentException("Text exceeds 10 chars: " + text);
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 0x20 || c > 0x7E || !Character.isLetterOrDigit(c)) {
                throw new IllegalArgumentException("Text must be printable alphanumeric: " + text);
            }
        }
    }

    public static void validateClientOrderId(long clientOrderId) {
        if (clientOrderId < ClientOrderIdAllocator.MIN || clientOrderId > ClientOrderIdAllocator.MAX) {
            throw new IllegalArgumentException("Client Order ID out of range: " + clientOrderId);
        }
    }

    /** Leading zeros are rejected for Security ID / Broker ID / Trade Report ID. */
    public static void validateNoLeadingZeros(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Identifier must not be empty");
        }
        if (value.length() > 1 && value.charAt(0) == '0') {
            throw new IllegalArgumentException("Leading zeros not allowed: " + value);
        }
    }

    public static void validateSide(long side) {
        if (side != 1 && side != 2 && side != 5) {
            throw new IllegalArgumentException("Invalid Side: " + side);
        }
    }

    public static void validateOrderType(long orderType) {
        if (orderType != 1 && orderType != 2) {
            throw new IllegalArgumentException("Invalid Order Type: " + orderType);
        }
    }

    public static void validateTif(long tif) {
        if (tif != 0 && tif != 3 && tif != 4 && tif != 9) {
            throw new IllegalArgumentException("Invalid TIF: " + tif);
        }
    }
}
