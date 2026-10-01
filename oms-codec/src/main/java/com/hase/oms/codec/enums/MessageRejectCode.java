package com.hase.oms.codec.enums;

/** Values for field {@code messageRejectCode} (HKEX data dictionary). */
public enum MessageRejectCode {
    RequiredFieldMissing(1),
    FieldNotDefined(2),
    UndefinedField(3),
    FieldWithoutValue(4),
    IncorrectValue(5),
    IncorrectDataFormat(6),
    CompIdProblem(9),
    InvalidMessageType(11),
    FieldAppearsMoreThanOnce(13),
    Other(99);

    private final long code;

    MessageRejectCode(long code) { this.code = code; }

    public long code() { return code; }
}
