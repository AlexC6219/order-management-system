package com.hase.oms.codec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A decoded (or to-be-encoded) OCG-C message.
 *
 * <p>Body fields are held by name in a {@link LinkedHashMap}. Field value types:
 * <ul>
 *   <li>integer types (uint8/int8/uint16/...) - {@link Long}</li>
 *   <li>{@code decimal} - {@link Long} (scaled by 1e8)</li>
 *   <li>{@code byte} - {@link String} of length 1 (the ASCII character)</li>
 *   <li>{@code alpha-fixed}/{@code alpha-var} - {@link String}</li>
 *   <li>{@code bitmap-*} - {@code byte[]}</li>
 * </ul>
 */
public final class Message {

    private final int messageType;
    private long sequenceNumber = 1;
    private int possDup = 0;
    private int possResend = 0;
    private String compId = "";
    private final LinkedHashMap<String, Object> fields = new LinkedHashMap<>();

    public Message(int messageType) {
        this.messageType = messageType;
    }

    public int messageType() {
        return messageType;
    }

    public Message sequenceNumber(long seq) {
        this.sequenceNumber = seq;
        return this;
    }

    public long sequenceNumber() {
        return sequenceNumber;
    }

    public Message possDup(int v) {
        this.possDup = v;
        return this;
    }

    public int possDup() {
        return possDup;
    }

    public Message possResend(int v) {
        this.possResend = v;
        return this;
    }

    public int possResend() {
        return possResend;
    }

    public Message compId(String id) {
        this.compId = id;
        return this;
    }

    public String compId() {
        return compId;
    }

    public Message put(String field, Object value) {
        fields.put(field, value);
        return this;
    }

    public Object get(String field) {
        return fields.get(field);
    }

    public boolean has(String field) {
        return fields.containsKey(field);
    }

    public Map<String, Object> fields() {
        return fields;
    }

    @Override
    public String toString() {
        return "Message{type=" + messageType + ", seq=" + sequenceNumber
                + ", compId='" + compId + "', fields=" + fields + "}";
    }
}
