package com.example.passman.util;

import java.security.SecureRandom;

public class PasswordGenerator {
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%&*()-_=+[]{};:,.<>?";

    private static final SecureRandom RAND = new SecureRandom();

    public static String generate(int length, boolean useUpper, boolean useDigits, boolean useSymbols, boolean avoidAmbiguous) {
        StringBuilder pool = new StringBuilder();
        pool.append(LOWER);
        if (useUpper) pool.append(UPPER);
        if (useDigits) pool.append(DIGITS);
        if (useSymbols) pool.append(SYMBOLS);

        String poolStr = pool.toString();
        if (avoidAmbiguous) poolStr = poolStr.replaceAll("[O0Il1]", "");

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = RAND.nextInt(poolStr.length());
            sb.append(poolStr.charAt(idx));
        }
        return sb.toString();
    }
}
