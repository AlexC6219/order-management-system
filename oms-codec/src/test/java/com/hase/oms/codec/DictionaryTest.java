package com.hase.oms.codec;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural validation of the data dictionary — the single source of truth.
 * These tests catch extraction errors (missing fields, duplicate bit positions,
 * dangling references) before they ever reach the wire.
 */
class DictionaryTest {

    private static Dictionary dict() {
        return Dictionary.load(
                DictionaryTest.class.getResourceAsStream("/fields.yaml"),
                DictionaryTest.class.getResourceAsStream("/messages.yaml"));
    }

    @Test
    void fieldDictionaryIsComplete() {
        Dictionary dict = dict();
        assertEquals(124, dict.allFields().size(), "expected 124 body fields (spec §8.2)");
    }

    @Test
    void messageDictionaryIsComplete() {
        Dictionary dict = dict();
        assertEquals(27, dict.allMessages().size(), "expected 27 message types (spec §7.1)");

        Set<Integer> types = dict.allMessages().stream()
                .map(Dictionary.MessageDef::type)
                .collect(Collectors.toSet());
        // valid types are 0..18 then 21..28 (no 19/20)
        for (int t = 0; t <= 18; t++) {
            assertTrue(types.contains(t), "missing message type " + t);
        }
        for (int t = 21; t <= 28; t++) {
            assertTrue(types.contains(t), "missing message type " + t);
        }
        assertTrue(!types.contains(19) && !types.contains(20), "19/20 must be absent");
    }

    @Test
    void everyReferencedFieldExists() {
        Dictionary dict = dict();
        Set<String> fieldNames = dict.allFields().stream()
                .map(Dictionary.FieldDef::name)
                .collect(Collectors.toSet());

        for (Dictionary.MessageDef m : dict.allMessages()) {
            for (Dictionary.FieldEntry e : m.fields()) {
                assertTrue(fieldNames.contains(e.field()),
                        m.name() + " references unknown field '" + e.field() + "'");
            }
        }
    }

    @Test
    void noDuplicateBitPositionWithinAMessage() {
        Dictionary dict = dict();
        for (Dictionary.MessageDef m : dict.allMessages()) {
            Set<Integer> seen = new HashSet<>();
            for (Dictionary.FieldEntry e : m.fields()) {
                assertTrue(seen.add(e.bit()),
                        m.name() + " has duplicate bit position " + e.bit());
            }
        }
    }

    @Test
    void headerLayoutIsFiftyFourBytes() {
        // 1 (STX) + 2 (Length) + 1 (MsgType) + 4 (Seq) + 1 (PossDup) + 1 (PossResend)
        // + 12 (CompId) + 32 (presence map) = 54
        assertEquals(54, Dictionary.HEADER_BYTES);
        assertEquals(32, Dictionary.PRESENCE_MAP_BYTES);
        assertEquals(4, Dictionary.TRAILER_BYTES);
    }
}
