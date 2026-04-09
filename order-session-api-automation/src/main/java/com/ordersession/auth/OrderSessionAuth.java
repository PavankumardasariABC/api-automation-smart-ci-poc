package com.ordersession.auth;

import com.ordersession.config.ConfigManager;
import com.ordersession.store.ResponseStore;
import io.qameta.allure.Allure;

/**
 * Bearer token for Order Session API.
 * <p>
 * <b>Suite flow:</b> {@link #prefetchBearerForSuite()} runs from {@code BaseTestTemplate} {@code @BeforeSuite}
 * so the authorization API runs <em>first</em> (when JWT / static token / OAuth merge file is configured),
 * the access token is stored as {@code OrderSessionAccessToken} in {@link ResponseStore}, and every call to
 * {@link #bearerToken()} reuses that cache for Order Session requests (see {@code headersWithBearer} in tests).
 * </p>
 * <p>Precedence for resolving the token:</p>
 * <ol>
 *   <li>{@code ORDER_SESSION_JWT} environment variable</li>
 *   <li>System property {@code -Dauth.bearer.token}</li>
 *   <li>Module {@code auth.bearer.token} if set and not a placeholder</li>
 *   <li>Cached {@code OrderSessionAccessToken} in {@link ResponseStore}</li>
 *   <li>Fetch via OAuth — {@code order.session.auth.source} =
 *       {@code GLOFOX} (default), {@code CLIENT}, or {@code SECURE} using merged file from {@code external.api.env.path}</li>
 * </ol>
 */
public final class OrderSessionAuth {

    private static final Object FETCH_LOCK = new Object();

    private OrderSessionAuth() {
    }

    /**
     * True when the suite can obtain a bearer: env JWT, {@code -Dauth.bearer.token}, non-placeholder
     * {@code auth.bearer.token} in properties, a readable OAuth merge file ({@code external.api.env.path}),
     * or enough {@code order.session.auth.source} settings (CLIENT / SECURE / GLOFOX) for
     * {@link ExternalApiTokenProvider#fetchFromConfiguredSource()} (same as {@link #bearerToken()} fallback).
     */
    public static boolean isAuthConfigured() {
        String env = System.getenv("ORDER_SESSION_JWT");
        if (env != null && !env.isBlank()) {
            return true;
        }
        String sys = System.getProperty("auth.bearer.token");
        if (sys != null && !sys.isBlank()) {
            return true;
        }
        String fileTok = ConfigManager.getOptional("auth.bearer.token");
        if (fileTok != null && !isPlaceholderToken(fileTok)) {
            return true;
        }
        if (ConfigManager.hasExternalMerge()) {
            return true;
        }
        return hasOAuthFetchPrerequisites();
    }

    /**
     * Mirrors {@link ExternalApiTokenProvider#fetchFromConfiguredSource()} prerequisites so
     * {@link #isAuthConfigured()} matches cases where {@link #bearerToken()} can still fetch (e.g.
     * {@code auth.url} + {@code credentials} in {@code env/qa.properties} without a merge file).
     */
    private static boolean hasOAuthFetchPrerequisites() {
        String mode = ConfigManager.getOptional("order.session.auth.source");
        if (mode == null || mode.isBlank()) {
            mode = "GLOFOX";
        }
        return switch (mode.trim().toUpperCase()) {
            case "CLIENT" -> nonBlank(ConfigManager.getOptional("auth.url"))
                    && clientCredentialsPresent();
            case "SECURE" -> nonBlank(ConfigManager.getOptional("secure.auth.url"))
                    && nonBlank(ConfigManager.getOptional("secure.username"))
                    && nonBlank(ConfigManager.getOptional("secure.password"));
            default -> nonBlank(ConfigManager.getOptional("base.url"));
        };
    }

    private static boolean clientCredentialsPresent() {
        String sys = System.getProperty("auth.creds");
        if (sys != null && !sys.isBlank()) {
            return true;
        }
        return nonBlank(ConfigManager.getOptional("credentials"));
    }

    private static boolean nonBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Runs once at suite start: calls OAuth / client-credentials (or uses JWT) and caches the access token
     * in {@link ResponseStore} under {@code OrderSessionAccessToken} so all later
     * {@link #bearerToken()} calls reuse it. No-op when auth is not configured; does not fail the suite if
     * prefetch fails (individual tests may still skip or fail).
     */
    public static void prefetchBearerForSuite() {
        if (!isAuthConfigured()) {
            System.out.println(
                    "ℹ️ OrderSessionAuth: no JWT / static token / OAuth merge — skipping bearer prefetch "
                            + "(401-only tests can still run)");
            return;
        }
        try {
            String token = bearerToken();
            if (!isPlaceholderToken(token)) {
                System.out.println(
                        "✅ OrderSessionAuth: authorization token obtained and cached for Order Session API calls");
            }
        } catch (Exception e) {
            System.err.println("⚠️ OrderSessionAuth: bearer prefetch failed (tests may skip or retry): " + e.getMessage());
        }
    }

    public static String bearerToken() {
        String env = System.getenv("ORDER_SESSION_JWT");
        if (env != null && !env.isBlank()) {
            Allure.step("Bearer: ORDER_SESSION_JWT");
            cache(env.trim());
            return env.trim();
        }
        String sys = System.getProperty("auth.bearer.token");
        if (sys != null && !sys.isBlank()) {
            Allure.step("Bearer: -Dauth.bearer.token");
            cache(sys.trim());
            return sys.trim();
        }
        String fileToken = ConfigManager.getOptional("auth.bearer.token");
        if (fileToken != null && !isPlaceholderToken(fileToken)) {
            Allure.step("Bearer: auth.bearer.token from properties");
            cache(fileToken);
            return fileToken;
        }

        String cached = ResponseStore.get("OrderSessionAccessToken");
        if (!isPlaceholderToken(cached)) {
            return cached;
        }

        synchronized (FETCH_LOCK) {
            cached = ResponseStore.get("OrderSessionAccessToken");
            if (!isPlaceholderToken(cached)) {
                return cached;
            }
            Allure.step("Bearer: fetch from external auth APIs (order.session.auth.source)");
            String fetched = ExternalApiTokenProvider.fetchFromConfiguredSource();
            cache(fetched);
            return fetched;
        }
    }

    private static void cache(String token) {
        if (!isPlaceholderToken(token)) {
            ResponseStore.put("OrderSessionAccessToken", token);
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
}
