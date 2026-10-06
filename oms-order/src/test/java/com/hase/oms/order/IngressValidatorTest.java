package com.hase.oms.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IngressValidatorTest {

    @Test
    void textLengthAndCharacters() {
        assertDoesNotThrow(() -> IngressValidator.validateText("ABC123"));
        assertDoesNotThrow(() -> IngressValidator.validateText(""));
        assertThrows(IllegalArgumentException.class, () -> IngressValidator.validateText("ABCDEFGHIJK"));
        assertThrows(IllegalArgumentException.class, () -> IngressValidator.validateText("BAD!"));
    }

    @Test
    void leadingZerosRejected() {
        assertDoesNotThrow(() -> IngressValidator.validateNoLeadingZeros("5"));
        assertThrows(IllegalArgumentException.class, () -> IngressValidator.validateNoLeadingZeros("05"));
    }

    @Test
    void enumerations() {
        assertDoesNotThrow(() -> IngressValidator.validateSide(5));
        assertThrows(IllegalArgumentException.class, () -> IngressValidator.validateSide(3));
        assertDoesNotThrow(() -> IngressValidator.validateOrderType(2));
        assertThrows(IllegalArgumentException.class, () -> IngressValidator.validateOrderType(9));
        assertDoesNotThrow(() -> IngressValidator.validateTif(9));
        assertThrows(IllegalArgumentException.class, () -> IngressValidator.validateTif(1));
    }
}
