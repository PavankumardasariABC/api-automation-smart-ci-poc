package com.billing.auth;

import com.billing.config.ConfigManager;
import com.billing.store.ResponseStore;
import io.qameta.allure.Allure;

/**
 * Caches bearer token for Billing API. Suite prefetch stores {@value #CACHE_KEY}.
 */
public final class BillingAuth {

    public static final String CACHE_KEY = "BillingAccessToken";

    private static final Object FETCH_LOCK = new Object();

    private BillingAuth() {
    }

    public static void prefetchBearerForSuite() {
        try {
            String token = bearerToken();
            if (!isPlaceholderToken(token)) {
                System.out.println("✅ BillingAuth: bearer cached for Billing API calls");
            }
        } catch (Exception e) {
            System.err.println("⚠️ BillingAuth: prefetch failed — " + e.getMessage());
        }
    }

    public static String bearerToken() {
        String env = System.getenv("BILLING_COMMERCE_JWT");
        if (env != null && !env.isBlank() && !isPlaceholderToken(env)) {
            Allure.step("Bearer: BILLING_COMMERCE_JWT");
            cache(env.trim());
            return env.trim();
        }
        String sys = System.getProperty("auth.bearer.token");
        if (sys != null && !sys.isBlank() && !isPlaceholderToken(sys)) {
            Allure.step("Bearer: -Dauth.bearer.token");
            cache(sys.trim());
            return sys.trim();
        }
        String fileTok = ConfigManager.getOptional("auth.bearer.token");
        if (fileTok != null && !isPlaceholderToken(fileTok)) {
            Allure.step("Bearer: auth.bearer.token from properties");
            cache(fileTok);
            return fileTok;
        }

        String cached = ResponseStore.get(CACHE_KEY);
        if (!isPlaceholderToken(cached)) {
            return cached;
        }

        synchronized (FETCH_LOCK) {
            cached = ResponseStore.get(CACHE_KEY);
            if (!isPlaceholderToken(cached)) {
                return cached;
            }
            Allure.step("Bearer: Commerce client_credentials");
            String fetched = BillingTokenProvider.fetchAccessToken();
            cache(fetched);
            return fetched;
        }
    }

    private static void cache(String token) {
        if (!isPlaceholderToken(token)) {
            ResponseStore.put(CACHE_KEY, token);
        }
    }

    public static boolean isPlaceholderToken(String token) {
        if (token == null) {
            return true;
        }
        String t = token.trim();
        return t.isEmpty()
                || t.startsWith("REPLACE_")
                || "__FETCH_FROM_EXTERNAL__".equalsIgnoreCase(t)
                || "PASTE_JWT_HERE".equalsIgnoreCase(t);
    }

    /** True when JWT env/system/property is set, or OAuth password is available for client_credentials. */
    public static boolean canResolveBearer() {
        if (!isPlaceholderToken(System.getenv("BILLING_COMMERCE_JWT"))) {
            return true;
        }
        String sys = System.getProperty("auth.bearer.token");
        if (sys != null && !sys.isBlank() && !isPlaceholderToken(sys)) {
            return true;
        }
        String fileTok = ConfigManager.getOptional("auth.bearer.token");
        if (fileTok != null && !isPlaceholderToken(fileTok)) {
            return true;
        }
        String envPass = System.getenv("BILLING_OAUTH_PASSWORD");
        if (envPass != null && !envPass.isBlank()) {
            return true;
        }
        String pass = ConfigManager.getOptional("billing.oauth.password");
        return pass != null && !pass.isBlank();
    }
}
