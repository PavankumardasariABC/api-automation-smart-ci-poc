package com.membertransfer.support;

import com.membertransfer.config.ApiClient;
import com.membertransfer.config.ConfigManager;
import com.membertransfer.store.ResponseStore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.qameta.allure.Allure;
import io.restassured.response.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Endpoints, headers, and token flow for member-transfer bulk APIs.
 */
public final class MemberTransferSupport {

    public static final String ACCESS_TOKEN_KEY = "MemberTransferAccessToken";
    public static final String BULK_ID_KEY = "MemberTransferBulkId";

    private static final String INQUIRY_PATH = "/api/member-transfer/billing-account-batch-transfers/inquiry";
    private static final String STATUS_PATH_TEMPLATE = "/api/member-transfer/billing-account-batch-transfers/status/%s";
    /**
     * GET paged list for a bulk (not the {@code /status/...} sub-resource).
     */
    private static final String BATCH_TRANSFERS_BY_BULK_ID_TEMPLATE =
            "/api/member-transfer/billing-account-batch-transfers/%s";

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private MemberTransferSupport() {
    }

    public static String baseUrl() {
        return ConfigManager.get("base.url").replaceAll("/$", "");
    }

    public static String authTokenUrl() {
        return ConfigManager.getAuthUrl();
    }

    public static String inquiryUrl() {
        return baseUrl() + INQUIRY_PATH;
    }

    public static String statusUrl(String bulkId) {
        return statusUrl(bulkId, 0, 10, "operation_type==transfer", "toBillingAccountNumber,asc");
    }

    public static String statusUrl(String bulkId, int page, int size, String q, String sort) {
        String encodedQ = URLEncoder.encode(q, StandardCharsets.UTF_8);
        String encodedSort = URLEncoder.encode(sort, StandardCharsets.UTF_8);
        return baseUrl() + String.format(STATUS_PATH_TEMPLATE, bulkId)
                + "?page=" + page
                + "&size=" + size
                + "&q=" + encodedQ
                + "&sort=" + encodedSort;
    }

    /**
     * GET /billing-account-batch-transfers/{bulkId}?q=...&page=...&size=...
     * (e.g. filter {@code operation_type==TRANSFER} for transfer operations.)
     */
    public static String billingAccountBatchTransfersByBulkIdUrl(String bulkId) {
        return billingAccountBatchTransfersByBulkIdUrl(bulkId, 0, 10, "operation_type==TRANSFER");
    }

    public static String billingAccountBatchTransfersByBulkIdUrl(String bulkId, int page, int size, String q) {
        String encodedQ = URLEncoder.encode(q, StandardCharsets.UTF_8);
        return baseUrl() + String.format(BATCH_TRANSFERS_BY_BULK_ID_TEMPLATE, bulkId)
                + "?q=" + encodedQ
                + "&page=" + page
                + "&size=" + size;
    }

    /**
     * {@code ?q=...} only (no {@code page}/{@code size}) for default-paging behavior checks.
     */
    public static String billingAccountBatchTransfersByBulkIdUrlQOnly(String bulkId, String q) {
        String encodedQ = URLEncoder.encode(q, StandardCharsets.UTF_8);
        return baseUrl() + String.format(BATCH_TRANSFERS_BY_BULK_ID_TEMPLATE, bulkId) + "?q=" + encodedQ;
    }

    /**
     * {@code ?page=…&size=…} only (no {@code q}).
     */
    public static String billingAccountBatchTransfersByBulkIdUrlPageSizeOnly(String bulkId, int page, int size) {
        return baseUrl() + String.format(BATCH_TRANSFERS_BY_BULK_ID_TEMPLATE, bulkId)
                + "?page=" + page + "&size=" + size;
    }

    /**
     * Appends a raw query string (without leading {@code ?}). Use for encoding-edge tests when needed.
     */
    public static String billingAccountBatchTransfersByBulkIdUrlRawQuery(String bulkId, String queryWithoutQuestion) {
        return baseUrl() + String.format(BATCH_TRANSFERS_BY_BULK_ID_TEMPLATE, bulkId) + "?" + queryWithoutQuestion;
    }

    public static String tenantId() {
        return ConfigManager.get("member.transfer.abcfs.tenant.id");
    }

    public static String memberTransferAuthCredentials() {
        String fromSystemProperty = System.getProperty("member.transfer.auth.credentials");
        if (fromSystemProperty != null && !fromSystemProperty.isBlank()) {
            return fromSystemProperty.trim();
        }
        return ConfigManager.getMemberTransferAuthCredentials();
    }

    public static String inquiryUserName() {
        return ConfigManager.get("member.transfer.inquiry.user.name");
    }

    public static String fromBillingAccountId() {
        return ConfigManager.get("member.transfer.inquiry.from.billing.account.id");
    }

    public static String toLocationId() {
        return ConfigManager.get("member.transfer.inquiry.to.location.id");
    }

    public static String reasonCodeId() {
        String v = ConfigManager.getOptional("member.transfer.inquiry.reason.code.id");
        return v != null ? v : "";
    }

    public static String sampleStatusBulkId() {
        return ConfigManager.get("member.transfer.status.sample.bulk.id");
    }

    public static Map<String, String> formAuthHeaders() {
        String creds = memberTransferAuthCredentials();
        if (creds == null || creds.isBlank()) {
            throw new IllegalStateException("member.transfer.auth.credentials is missing");
        }
        String encodedCreds = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Basic " + encodedCreds);
        headers.put("User-Agent", "RestAssured-MemberTransfer/1.0");
        headers.put("Accept", "application/json");
        headers.put("Cache-Control", "no-cache");
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        return headers;
    }

    public static Map<String, String> bearerJsonHeaders() {
        return bearerJsonHeaders(accessToken());
    }

    public static Map<String, String> bearerJsonHeaders(String bearerToken) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + bearerToken);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept", "application/json;charset=UTF-8");
        headers.put("ABCFS-TENANT-ID", tenantId());
        return headers;
    }

    public static String accessToken() {
        return Optional.ofNullable(ResponseStore.get(ACCESS_TOKEN_KEY))
                .map(Object::toString)
                .filter(s -> !s.isBlank() && !"null".equalsIgnoreCase(s))
                .orElseGet(MemberTransferSupport::fetchAndStoreToken);
    }

    public static String fetchAndStoreToken() {
        String url = authTokenUrl();
        Map<String, String> headers = formAuthHeaders();
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        Allure.step("Request OAuth token (client_credentials) for member-transfer");
        Response response = ApiClient.post(url, headers, body);
        if (response.getStatusCode() != 200) {
            Allure.step("Retry: token call returned " + response.getStatusCode());
            response = ApiClient.post(url, headers, body);
        }
        if (response.getStatusCode() != 200) {
            throw new IllegalStateException("Token HTTP " + response.getStatusCode() + ": " + response.asString());
        }
        String access = response.jsonPath().getString("access_token");
        if (access == null || access.isEmpty()) {
            throw new IllegalStateException("No access_token in response");
        }
        ResponseStore.put(ACCESS_TOKEN_KEY, access);
        return access;
    }

    public static String defaultInquiryBodyJson() {
        Map<String, Object> firstItem = new LinkedHashMap<>();
        firstItem.put("fromBillingAccountId", fromBillingAccountId());
        firstItem.put("toLocationId", toLocationId());
        String rc = reasonCodeId();
        if (rc != null && !rc.isBlank()) {
            firstItem.put("reasonCodeId", rc);
        }
        firstItem.put("brandRulesPreference", defaultBrandRules());

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("userName", inquiryUserName());
        root.put("items", List.of(firstItem));
        return GSON.toJson(root);
    }

    /**
     * True when a default happy-path inquiry can be sent (no {@code REPLACE_} in OAuth or required inquiry/tenant config).
     */
    public static boolean isInquiryRequestConfigured() {
        try {
            if (memberTransferAuthCredentials().contains("REPLACE")) {
                return false;
            }
            if (ConfigManager.get("member.transfer.inquiry.from.billing.account.id").contains("REPLACE")) {
                return false;
            }
            if (ConfigManager.get("member.transfer.inquiry.to.location.id").contains("REPLACE")) {
                return false;
            }
            if (ConfigManager.get("member.transfer.abcfs.tenant.id").contains("REPLACE")) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * POST {@link #inquiryUrl()} with {@link #defaultInquiryBodyJson()}. On HTTP 200, stores the returned
     * {@code bulkId} in {@link ResponseStore} as {@link #BULK_ID_KEY} (same flow as bulk inquiry API tests).
     * Retries the POST after a fresh token on 401. Returns {@code null} if configuration is incomplete, the call
     * is not 200, or no {@code bulkId} in the body.
     */
    public static String postInquiryAndStoreBulkId() {
        if (!isInquiryRequestConfigured()) {
            return null;
        }
        if (ResponseStore.get(ACCESS_TOKEN_KEY) == null) {
            fetchAndStoreToken();
        }
        String t = (String) ResponseStore.get(ACCESS_TOKEN_KEY);
        if (t == null || t.isEmpty()) {
            t = accessToken();
        }
        String url = inquiryUrl();
        String body = defaultInquiryBodyJson();
        Response r = ApiClient.post(url, bearerJsonHeaders(t), body);
        if (r.getStatusCode() == 401) {
            fetchAndStoreToken();
            t = (String) ResponseStore.get(ACCESS_TOKEN_KEY);
            r = ApiClient.post(url, bearerJsonHeaders(t), body);
        }
        if (r.getStatusCode() != 200) {
            String snippet = r.asString();
            if (snippet != null && snippet.length() > 500) {
                snippet = snippet.substring(0, 500) + "…";
            }
            Allure.step("postInquiryAndStoreBulkId: HTTP " + r.getStatusCode()
                    + (snippet == null || snippet.isEmpty() ? "" : " — " + snippet));
            return null;
        }
        String bulk = r.jsonPath().getString("bulkId");
        if (bulk == null || bulk.isBlank()) {
            return null;
        }
        ResponseStore.put(BULK_ID_KEY, bulk);
        return bulk;
    }

    public static Map<String, Object> defaultBrandRules() {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("pastDueTransferEnabled", true);
        b.put("pendingCancellationSubscriptionsEnabled", true);
        b.put("paidUpFrontBalanceTransferEnabled", true);
        return b;
    }

    public static String toJson(Object map) {
        return GSON.toJson(map);
    }
}
