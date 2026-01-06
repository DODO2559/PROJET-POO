package com.example.passman.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.AEADBadTagException;

import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtil {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;
    private static final int AES_KEY_BITS = 256;
    private static final int PBKDF2_ITER = 200_000; // solide pour démo
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    public static String generateSaltBase64() {
        byte[] s = new byte[SALT_BYTES];
        RANDOM.nextBytes(s);
        return Base64.getEncoder().encodeToString(s);
    }

    public static String hashPassword(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITER, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verifyPassword(char[] password, byte[] salt, String expectedHashBase64) throws Exception {
        String h = hashPassword(password, salt);
        return h.equals(expectedHashBase64);
    }

    // Derive AES key bytes from password + salt
    private static SecretKey deriveKey(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITER, AES_KEY_BITS);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = skf.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static class Encrypted {
        public final String ciphertextBase64;
        public final String ivBase64;
        public Encrypted(String c, String iv) { this.ciphertextBase64 = c; this.ivBase64 = iv; }
    }

    public static Encrypted encrypt(char[] password, String plaintext, byte[] saltForKey) throws Exception {
        SecretKey key = deriveKey(password, saltForKey);
        byte[] iv = new byte[GCM_IV_LENGTH];
        RANDOM.nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        c.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] cipherBytes = c.doFinal(plaintext.getBytes("UTF-8"));
        return new Encrypted(Base64.getEncoder().encodeToString(cipherBytes), Base64.getEncoder().encodeToString(iv));
    }

    public static String decrypt(char[] password, String ciphertextBase64, byte[] saltForKey, String ivBase64) throws Exception {
        SecretKey key = deriveKey(password, saltForKey);
        byte[] iv = Base64.getDecoder().decode(ivBase64);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        c.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] plain = c.doFinal(Base64.getDecoder().decode(ciphertextBase64));
        return new String(plain, "UTF-8");
    }
}
