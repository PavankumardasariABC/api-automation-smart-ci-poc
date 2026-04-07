package com.billing.service;

import com.billing.config.ApiClient;
import com.billing.config.ConfigManager;
import io.restassured.response.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rest Assured facade for Billing OpenAPI paths (base: {@code billing.base.url}).
 */
public final class BillingApiService {

    private BillingApiService() {
    }

    private static String base() {
        return ConfigManager.get("billing.base.url").replaceAll("/$", "");
    }

    public static Response getCancelCodes(Map<String, String> headers, int page, int size) {
        String url = base() + "/cancel-codes?page=" + page + "&size=" + size;
        return ApiClient.get(url, headers);
    }

    public static Response getCancelCodeById(Map<String, String> headers, String id) {
        return ApiClient.get(base() + "/cancel-codes/" + id, headers);
    }

    public static Response getAdjustmentCodes(Map<String, String> headers, int page, int size) {
        return ApiClient.get(base() + "/adjustment-codes?page=" + page + "&size=" + size, headers);
    }

    public static Response getTransferCodes(Map<String, String> headers, int page, int size) {
        return ApiClient.get(base() + "/transfer-codes?page=" + page + "&size=" + size, headers);
    }

    public static Response getAdjustmentCodeById(Map<String, String> headers, String id) {
        return ApiClient.get(base() + "/adjustment-codes/" + id, headers);
    }

    public static Response getTransferCodeById(Map<String, String> headers, String id) {
        return ApiClient.get(base() + "/transfer-codes/" + id, headers);
    }

    public static Response getClientProfiles(Map<String, String> headers, int page, int size) {
        return ApiClient.get(base() + "/client-profiles?page=" + page + "&size=" + size, headers);
    }

    public static Response getClientProfileById(Map<String, String> headers, String id) {
        return ApiClient.get(base() + "/client-profiles/" + id, headers);
    }

    public static Response getBillingAccounts(Map<String, String> headers, int page, int size, String rsql) {
        StringBuilder sb = new StringBuilder(base()).append("/billing-accounts?page=").append(page).append("&size=").append(size);
        if (rsql != null && !rsql.isBlank()) {
            sb.append("&q=").append(java.net.URLEncoder.encode(rsql, java.nio.charset.StandardCharsets.UTF_8));
        }
        return ApiClient.get(sb.toString(), headers);
    }

    public static Response getBillingAccountById(Map<String, String> headers, String billingAccountId) {
        return ApiClient.get(base() + "/billing-accounts/" + billingAccountId, headers);
    }

    public static Response getSubscriptions(Map<String, String> headers, String billingAccountId, int page, int size) {
        return ApiClient.get(base() + "/billing-accounts/" + billingAccountId + "/subscriptions?page=" + page + "&size=" + size, headers);
    }

    public static Response getSubscriptionById(Map<String, String> headers, String billingAccountId, String subscriptionId) {
        return ApiClient.get(base() + "/billing-accounts/" + billingAccountId + "/subscriptions/" + subscriptionId, headers);
    }

    public static Response getTransfersForAccount(Map<String, String> headers, String billingAccountId, int page, int size) {
        return ApiClient.get(base() + "/billing-accounts/" + billingAccountId + "/transfers?page=" + page + "&size=" + size, headers);
    }

    public static Response getCancellationsForAccount(Map<String, String> headers, String billingAccountId, int page, int size) {
        return ApiClient.get(base() + "/billing-accounts/" + billingAccountId + "/cancellations?page=" + page + "&size=" + size, headers);
    }

    public static Response getResetsForAccount(Map<String, String> headers, String billingAccountId, int page, int size) {
        return ApiClient.get(base() + "/billing-accounts/" + billingAccountId + "/resets?page=" + page + "&size=" + size, headers);
    }

    public static Response getNotesForAccount(Map<String, String> headers, String billingAccountId, int page, int size, String sort) {
        String url = base() + "/billing-accounts/" + billingAccountId + "/notes?page=" + page + "&size=" + size;
        if (sort != null && !sort.isBlank()) {
            url += "&sort=" + java.net.URLEncoder.encode(sort, java.nio.charset.StandardCharsets.UTF_8);
        }
        return ApiClient.get(url, headers);
    }

    public static Response getWalletEntryAssignments(Map<String, String> headers, int page, int size) {
        return ApiClient.get(base() + "/wallet-entry-assignments?page=" + page + "&size=" + size, headers);
    }

    public static Response getWalletEntryAssignmentById(Map<String, String> headers, String id) {
        return ApiClient.get(base() + "/wallet-entry-assignments/" + id, headers);
    }

    public static Response getBillingAccountTransactions(Map<String, String> headersWithLocation, String billingAccountId,
                                                       int page, int size) {
        String url = base() + "/billing-accounts/" + billingAccountId + "/billing-account-transactions?page=" + page + "&size=" + size;
        return ApiClient.get(url, headersWithLocation);
    }

    public static Response postBillingAccountTransferDryRun(Map<String, String> headers, String billingAccountId,
                                                            Map<String, Object> body, boolean dryRun) {
        String url = base() + "/billing-accounts/" + billingAccountId + "/transfers?dryRun=" + dryRun;
        return ApiClient.post(url, headers, body);
    }

    public static Response postCreateNote(Map<String, String> headers, String billingAccountId, Map<String, Object> body) {
        return ApiClient.post(base() + "/billing-accounts/" + billingAccountId + "/notes", headers, body);
    }

    public static Response postInvalidJsonBody(Map<String, String> headers, String path, String garbageBody) {
        Map<String, String> h = new LinkedHashMap<>(headers);
        h.put("Content-Type", "application/json;charset=UTF-8");
        return ApiClient.post(base() + path, h, garbageBody);
    }
}
