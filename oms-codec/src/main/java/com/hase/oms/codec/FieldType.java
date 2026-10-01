package com.hase.oms.codec;

/** OCG-C data types (HKEX spec §6.1). */
public enum FieldType {
    UINT8,
    INT8,
    UINT16,
    INT16,
    UINT32,
    INT32,
    UINT64,
    INT64,
    /** 8-byte signed little-endian integer, 8 implied decimal places. */
    DECIMAL,
    /** Single ASCII character (NOT a numeric byte). */
    BYTE,
    /** Null-terminated ASCII, fixed length (includes null). */
    ALPHA_FIXED,
    /** 2-byte UInt16 length prefix (includes null), then ASCII. */
    ALPHA_VAR,
    /** Fixed 32-byte bitmap (256 positions, MSB-first). */
    BITMAP_FIXED,
    /** Variable bitmap used inside repeating blocks. */
    BITMAP_VAR;

    public static FieldType fromString(String s) {
        return switch (s) {
            case "uint8" -> UINT8;
            case "int8" -> INT8;
            case "uint16" -> UINT16;
            case "int16" -> INT16;
            case "uint32" -> UINT32;
            case "int32" -> INT32;
            case "uint64" -> UINT64;
            case "int64" -> INT64;
            case "decimal" -> DECIMAL;
            case "byte" -> BYTE;
            case "alpha-fixed" -> ALPHA_FIXED;
            case "alpha-var" -> ALPHA_VAR;
            case "bitmap-fixed" -> BITMAP_FIXED;
            case "bitmap-var" -> BITMAP_VAR;
            default -> throw new IllegalArgumentException("Unknown field type: " + s);
        };
    }

    public boolean isInteger() {
        return switch (this) {
            case UINT8, INT8, UINT16, INT16, UINT32, INT32, UINT64, INT64, DECIMAL -> true;
            default -> false;
        };
    }
}
