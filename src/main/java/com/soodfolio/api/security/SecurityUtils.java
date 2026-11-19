package com.soodfolio.api.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Hashes an input string using standard SHA-256 and returns a 64-character lowercase hex string.
     *
     * @param input Raw token or string to hash
     * @return 64-character hex-encoded SHA-256 hash
     */
    public static String sha256(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input string for SHA-256 hashing must not be null or blank");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
