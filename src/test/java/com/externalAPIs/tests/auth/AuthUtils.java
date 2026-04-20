package com.externalAPIs.tests.auth;

import io.qameta.allure.Allure;
import com.externalAPIs.store.ResponseStore;

/**
 * Utility class to fetch and cache authentication tokens for all API types:
 * Secure API, Client API, Glofox Client, and Glofox Secure Client APIs.
 * Updated to use Allure for reporting.
 */
public class AuthUtils {

    public static synchronized String getSecureClientToken() {
        String existing = ResponseStore.get("SecureClientToken");
        if (isValid(existing)) {
            log("🔐 Reusing cached SecureClientToken");
            return existing;
        }

        try {
            log("🔑 Generating new SecureClientToken...");
            Secure_TokenTests secureTokenTests = new Secure_TokenTests();
            secureTokenTests.getSecureAuthToken();
            String newToken = ResponseStore.get("SecureClientToken");

            if (!isValid(newToken))
                throw new IllegalStateException("SecureClientToken generation failed!");

            log("✅ SecureClientToken generated successfully");
            return newToken;

        } catch (Exception e) {
            log("⚠️ Failed to fetch SecureClientToken: " + e.getMessage());
            return "";
        }
    }

    public static synchronized String getClientToken() {
        String existing = ResponseStore.get("ClientToken");
        if (isValid(existing)) {
            log("🔐 Reusing cached ClientToken");
            return existing;
        }

        try {
            log("🔑 Generating new ClientToken...");
            Authorization_TokenTests tokenTests = new Authorization_TokenTests();
            tokenTests.getAuthToken();
            String newToken = ResponseStore.get("ClientToken");

            if (!isValid(newToken))
                throw new IllegalStateException("ClientToken generation failed!");

            log("✅ ClientToken generated successfully");
            return newToken;

        } catch (Exception e) {
            log("⚠️ Failed to fetch ClientToken: " + e.getMessage());
            return "";
        }
    }

    public static synchronized String getGlofoxClientToken() {
        String existing = ResponseStore.get("GlofoxClientToken");
        if (isValid(existing)) {
            log("🔐 Reusing cached GlofoxClientToken");
            return existing;
        }

        try {
            log("🔑 Generating new GlofoxClientToken...");
            Glofox_ClientTokenTests glofoxClientTokenTests = new Glofox_ClientTokenTests();
            glofoxClientTokenTests.getGlofoxClientToken();
            String newToken = ResponseStore.get("GlofoxClientToken");

            if (!isValid(newToken))
                throw new IllegalStateException("GlofoxClientToken generation failed!");

            log("✅ GlofoxClientToken generated successfully");
            return newToken;

        } catch (Exception e) {
            log("⚠️ Failed to fetch GlofoxClientToken: " + e.getMessage());
            return "";
        }
    }

    public static synchronized String getGlofoxSecureClientToken() {
        String existing = ResponseStore.get("glofoxSecureClientToken");
        if (isValid(existing)) {
            log("🔐 Reusing cached GlofoxSecureClientToken");
            return existing;
        }

        try {
            log("🔑 Generating new GlofoxSecureClientToken...");
            Glofox_SecureClientTokenTests secureTokenTests = new Glofox_SecureClientTokenTests();
            secureTokenTests.getGlofoxSecureClientToken();
            String newToken = ResponseStore.get("glofoxSecureClientToken");

            if (!isValid(newToken))
                throw new IllegalStateException("GlofoxSecureClientToken generation failed!");

            log("✅ GlofoxSecureClientToken generated successfully");
            return newToken;

        } catch (Exception e) {
            log("⚠️ Failed to fetch GlofoxSecureClientToken: " + e.getMessage());
            return "";
        }
    }

    private static boolean isValid(String token) {
        return token != null && !token.isBlank() && !"null".equalsIgnoreCase(token.trim());
    }

    private static void log(String message) {
        System.out.println(message);
        Allure.step(message);
    }
}
