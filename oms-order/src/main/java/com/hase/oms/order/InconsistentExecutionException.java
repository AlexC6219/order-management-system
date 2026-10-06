package com.hase.oms.order;

/** Raised when an Execution Report's quantities are internally inconsistent. */
public class InconsistentExecutionException extends RuntimeException {
    public InconsistentExecutionException(String message) {
        super(message);
    }
}
