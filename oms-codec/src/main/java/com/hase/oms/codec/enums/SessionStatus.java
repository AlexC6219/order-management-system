package com.hase.oms.codec.enums;

/** Values for field {@code sessionStatus} (HKEX data dictionary). */
public enum SessionStatus {
    SessionActive(0),
    SessionPasswordChanged(1),
    SessionPasswordDueToExpire(2),
    NewPasswordNotCompliant(3),
    SessionLogoutComplete(4),
    InvalidUsernameOrPassword(5),
    AccountLocked(6),
    LogonNotAllowed(7),
    PasswordExpired(8),
    PasswordChangeRequired(100),
    Other(101);

    private final long code;

    SessionStatus(long code) { this.code = code; }

    public long code() { return code; }
}
