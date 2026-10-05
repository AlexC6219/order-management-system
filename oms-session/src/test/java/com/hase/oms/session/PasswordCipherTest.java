package com.hase.oms.session;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.spec.MGF1ParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordCipherTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-03-04T05:06:07Z"), ZoneOffset.UTC);

    @Test
    void encryptsTimestampPrefixedPasswordRecoverableByPrivateKey() throws Exception {
        KeyPair pair = PasswordCipher.generateKeyPair();
        PasswordCipher cipher = new PasswordCipher(pair.getPublic(), FIXED);

        String encoded = cipher.encrypt("Password1");

        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPPadding");
        OAEPParameterSpec spec = new OAEPParameterSpec(
                PasswordCipher.OAEP_DIGEST, "MGF1",
                new MGF1ParameterSpec(PasswordCipher.MGF1_DIGEST),
                PSource.PSpecified.DEFAULT);
        rsa.init(Cipher.DECRYPT_MODE, pair.getPrivate(), spec);
        byte[] plain = rsa.doFinal(Base64.getDecoder().decode(encoded));
        String recovered = new String(plain, StandardCharsets.US_ASCII);

        assertEquals("20260304050607Password1", recovered);
        assertTrue(recovered.startsWith("20260304050607"));
    }

    @Test
    void ciphertextIsNotPlaintext() {
        KeyPair pair = PasswordCipher.generateKeyPair();
        PasswordCipher cipher = new PasswordCipher(pair.getPublic(), FIXED);
        String encoded = cipher.encrypt("Password1");
        assertNotEquals("Password1", encoded);
        assertTrue(Base64.getDecoder().decode(encoded).length >= 256);
    }
}
