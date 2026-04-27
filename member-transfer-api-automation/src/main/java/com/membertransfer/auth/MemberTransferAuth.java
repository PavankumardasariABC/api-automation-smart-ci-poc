package com.membertransfer.auth;

import com.membertransfer.config.ConfigManager;
import com.membertransfer.store.ResponseStore;
import com.membertransfer.support.MemberTransferSupport;
import io.qameta.allure.Allure;

/**
 * Caches OAuth2 bearer token (client_credentials) in {@link ResponseStore} for member-transfer API calls.
 * Intended to run at suite start from {@link com.membertransfer.tests.BaseTestTemplate#beforeSuite()}.
 */
public final class MemberTransferAuth {

    private MemberTransferAuth() {
    }

    public static void prefetchBearerForSuite() {
        String creds = ConfigManager.getOptional("member.transfer.auth.credentials");
        if (creds == null || creds.isBlank() || creds.contains("REPLACE")) {
            System.out.println("ℹ️ MemberTransferAuth: skip prefetch — set member.transfer.* in "
                    + "src/test/resources/env/ or a .local.properties file");
            return;
        }
        try {
            String token = MemberTransferSupport.fetchAndStoreToken();
            if (token != null && !token.isBlank()) {
                System.out.println("✅ MemberTransferAuth: access token cached for member-transfer API");
            }
        } catch (Exception e) {
            System.err.println("⚠️ MemberTransferAuth: prefetch failed (tests may fetch later): " + e.getMessage());
        }
    }

    /** Ensures a token is available (uses cache or fetches). */
    public static String accessToken() {
        Allure.step("Member transfer access token (cached or new)");
        return MemberTransferSupport.accessToken();
    }
}
