package com.hase.oms.codec.enums;

/** Values for field {@code execRestatementReason} (HKEX data dictionary). */
public enum ExecRestatementReason {
    CancelOnTradingHalt(6),
    MarketOperation(8),
    UnsolicitedCancelOriginalOrder(100),
    OnBehalfOfSingleCancel(101),
    OnBehalfOfMassCancel(102),
    MassCancelledByBroker(103),
    CancelOnDisconnect(104),
    CancelBrokerSuspended(105),
    CancelParticipantSuspended(106),
    SystemCancel(107);

    private final long code;

    ExecRestatementReason(long code) { this.code = code; }

    public long code() { return code; }
}
