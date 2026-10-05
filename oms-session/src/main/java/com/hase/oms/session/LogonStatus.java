package com.hase.oms.session;

import com.hase.oms.codec.enums.SessionStatus;

/**
 * Classification of the OCG-C {@code Session Status} returned in a Logon reply
 * (DESIGN.md §7.4; data dictionary {@code sessionStatus}).
 */
public enum LogonStatus {

    /** Logon accepted; session may proceed. */
    ACCEPTED,

    /** Logon accepted but the password should be changed soon. */
    PASSWORD_CHANGE_RECOMMENDED,

    /** Logon refused until a new password is set. */
    NEW_PASSWORD_REQUIRED,

    /** Logon refused (bad credentials, locked, expired, not allowed, etc.). */
    REJECTED;

    /** Classifies a raw {@code sessionStatus} code. Unknown codes are rejected. */
    public static LogonStatus of(long sessionStatusCode) {
        SessionStatus status = lookup(sessionStatusCode);
        if (status == null) {
            return REJECTED;
        }
        return switch (status) {
            case SessionActive, SessionPasswordChanged -> ACCEPTED;
            case SessionPasswordDueToExpire -> PASSWORD_CHANGE_RECOMMENDED;
            case NewPasswordNotCompliant, PasswordChangeRequired -> NEW_PASSWORD_REQUIRED;
            default -> REJECTED;
        };
    }

    public boolean accepted() {
        return this == ACCEPTED || this == PASSWORD_CHANGE_RECOMMENDED;
    }

    private static SessionStatus lookup(long code) {
        for (SessionStatus status : SessionStatus.values()) {
            if (status.code() == code) {
                return status;
            }
        }
        return null;
    }
}
