package com.hase.oms.session;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Encrypts the OCG-C Logon password using JDK-native RSA/OAEP.
 *
 * <p>Per DESIGN.md §7.4 the plaintext is the UTC login time
 * ({@code YYYYMMDDHHMMSS}) immediately followed by the password, encrypted with
 * RSA-2048 / OAEP, rendered big-endian and base-64 encoded. The OAEP digest and
 * MGF1 hash are pinned here so they can be aligned with HKEX golden vectors in
 * one place.
 */
public final class PasswordCipher {

    /** OAEP main digest. */
    public static final String OAEP_DIGEST = "SHA-256";
    /** OAEP MGF1 hash. */
    public static final String MGF1_DIGEST = "SHA-256";

    private static final DateTimeFormatter LOGIN_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final PublicKey publicKey;
    private final Clock clock;

    public PasswordCipher(PublicKey publicKey, Clock clock) {
        this.publicKey = publicKey;
        this.clock = clock;
    }

    public PasswordCipher(PublicKey publicKey) {
        this(publicKey, Clock.systemUTC());
    }

    /**
     * Encrypts {@code loginTime + password} and returns the base-64 ciphertext.
     * The JDK already emits the ciphertext in big-endian, so no reordering is
     * needed before encoding.
     */
    public String encrypt(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }
        String plaintext = currentLoginTime() + password;
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            OAEPParameterSpec spec = new OAEPParameterSpec(
                    OAEP_DIGEST, "MGF1",
                    new MGF1ParameterSpec(MGF1_DIGEST),
                    PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, spec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.US_ASCII));
            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("RSA/OAEP encryption failed", e);
        }
    }

    private String currentLoginTime() {
        return LOGIN_TIME.format(clock.instant());
    }

    /** Generates an ephemeral 2048-bit RSA keypair (used by the mock server). */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to generate RSA keypair", e);
        }
    }

    /** Decodes an X.509-encoded public key. */
    public static PublicKey decodePublicKey(byte[] x509) {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(x509));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid RSA public key", e);
        }
    }
}
