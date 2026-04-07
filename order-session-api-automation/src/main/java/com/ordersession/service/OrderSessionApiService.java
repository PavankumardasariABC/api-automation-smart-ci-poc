package com.ordersession.service;

import com.ordersession.config.ApiClient;
import com.ordersession.config.ConfigManager;
import io.restassured.response.Response;

import java.util.Map;

/**
 * Hybrid layer: centralizes URL building and verbs for Order Session OpenAPI paths.
 */
public final class OrderSessionApiService {

    private OrderSessionApiService() {
    }

    private static String base() {
        return ConfigManager.get("order.session.base.url").replaceAll("/$", "");
    }

    public static Response postPaymentTokenSession(Map<String, String> headers, Object body) {
        return ApiClient.post(base() + "/payment-session/tokens", headers, body);
    }

    public static Response getPaymentTokenSession(Map<String, String> headers, String sessionId) {
        return ApiClient.get(base() + "/payment-session/tokens/" + sessionId, headers);
    }

    public static Response postWalletEntrySession(Map<String, String> headers, Object body) {
        return ApiClient.post(base() + "/payment-session/wallet-entries", headers, body);
    }

    public static Response getWalletEntrySession(Map<String, String> headers, String sessionId) {
        return ApiClient.get(base() + "/payment-session/wallet-entries/" + sessionId, headers);
    }
}
