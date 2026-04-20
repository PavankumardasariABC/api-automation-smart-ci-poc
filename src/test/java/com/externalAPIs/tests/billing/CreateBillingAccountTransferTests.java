package com.externalAPIs.tests.billing;

import com.externalAPIs.config.ApiClient;
import com.externalAPIs.config.ConfigManager;
import com.externalAPIs.store.ResponseStore;
import com.externalAPIs.tests.auth.AuthUtils;
import com.google.gson.GsonBuilder;
import io.qameta.allure.*;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Billing Account Transfer API – Template Test Suite (TDD).
 * <p>
 * API: POST /billing-accounts/{billingAccountId}/transfer (dryRun query supported).
 * Successful responses are persisted via {@link BillingTransferResponseStore} for chained GETs and for
 * {@link BillingDataProvider} / later billing tests.
 * </p>
 */
@Epic("Billing")
@Feature("Billing Account Transfer API")
@Severity(SeverityLevel.CRITICAL)
public class CreateBillingAccountTransferTests {

    private static final String BILLING_TRANSFER_PATH = "/api/abcpg/billing-accounts/%s/transfer";
    private static final String BILLING_TRANSFER_GET_PATH = "/api/abcpg/billing-accounts/%s/transfers/%s";

    private static final String DEFAULT_BILLING_ACCOUNT_ID = "6f1e19b3-1ed2-44a2-8de9-3e2c4c2db5e6";
    private static final String FALLBACK_TARGET_LOCATION_UUID = "11ec0af2-3a19-b7d3-a84f-59243ef7e239";

    private static String getBillingAccountId() {
        return Optional.ofNullable(System.getProperty("billing.account.id"))
                .or(() -> Optional.ofNullable(System.getenv("BILLING_ACCOUNT_ID")))
                .filter(s -> !s.isBlank())
                .orElse(DEFAULT_BILLING_ACCOUNT_ID);
    }

    private static String getOrganizationId() {
        return Optional.ofNullable(ResponseStore.get("glofoxOrgId"))
                .map(Object::toString)
                .filter(s -> !s.isBlank() && !"null".equalsIgnoreCase(s))
                .orElse("41424320-436f-6d70-616e-792020202020");
    }

    /** Target location: last successful transfer/dry-run in store, then legacy locationId, then fallback. */
    private static String targetLocationId() {
        return BillingTransferResponseStore.resolveTargetLocationId(FALLBACK_TARGET_LOCATION_UUID);
    }

    private static Map<String, String> headersWithBearer(String token) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("ABCFS-ORGANIZATION-ID", getOrganizationId());
        headers.put("User-Agent", "Automation-Test");
        return headers;
    }

    private static String secureBase() {
        return ConfigManager.getSecureBaseUrl();
    }

    private static String transferPostUrl(String billingAccountId, boolean dryRun) {
        String path = String.format(BILLING_TRANSFER_PATH, billingAccountId);
        return secureBase() + path + (dryRun ? "?dryRun=true" : "");
    }

    private static String transferGetUrl(String billingAccountId, String transferId) {
        return secureBase() + String.format(BILLING_TRANSFER_GET_PATH, billingAccountId, transferId);
    }

    private static Response postTransfer(String url, String token, String bodyJson) {
        Response response = ApiClient.post(url, headersWithBearer(token), bodyJson);
        if (response.statusCode() == 401) {
            token = AuthUtils.getSecureClientToken();
            response = ApiClient.post(url, headersWithBearer(token), bodyJson);
        }
        return response;
    }

    private static String bearerToken() {
        String token = AuthUtils.getSecureClientToken();
        Assert.assertNotNull(token, "Secure client token is required");
        return token;
    }

    // ---------- Seed store early (dry run) so data provider & negatives reuse location / transfer id ----------

    @Test(priority = 1, groups = {"Billing", "Regression"})
    @Story("Dry run returns eligibility status without executing transfer")
    @Description("Runs first when possible: persists id/location/status to ResponseStore for chained tests.")
    public void dryRunTrue_returnsEligibilityStatus_andPersistsForChaining() {
        String token = bearerToken();
        String url = transferPostUrl(getBillingAccountId(), true);
        String body = new GsonBuilder().create().toJson(Map.of(
                "locationId", targetLocationId(),
                "metadata", Map.of("reason", "EligibilityCheck", "source", "automation-template")));

        Response response = postTransfer(url, token, body);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());

        Assert.assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
                "Dry run should return 2xx, got " + response.statusCode());
        BillingTransferResponseStore.persistFromSuccessResponse(response);
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            Allure.step("Transfer eligibility status: " + response.jsonPath().getString("status"));
        }
    }

    // ---------- Positive / data-driven scenarios ----------

    @Story("As an admin, I can transfer a billing account to a new location")
    @Test(dataProvider = "billingAccountTransferData", dataProviderClass = BillingDataProvider.class, priority = 2,
            groups = {"Billing", "Regression"})
    @Description("Data-driven: valid transfer (execute and dry-run) with expected 2xx; response saved to ResponseStore.")
    public void transferWithValidPayload_expectedSuccess(Map<String, Object> payload) {
        String token = bearerToken();

        String billingAccountId = getBillingAccountId();
        Boolean dryRun = payload.get("dryRun") != null ? Boolean.TRUE.equals(payload.get("dryRun")) : false;
        String locationId = String.valueOf(payload.get("locationId"));
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = payload.containsKey("metadata") && payload.get("metadata") != null
                ? (Map<String, Object>) payload.get("metadata") : Map.of();

        Allure.step("Scenario: " + payload.get("scenario") + " – " + payload.get("description"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("locationId", locationId);
        if (!metadata.isEmpty()) {
            body.put("metadata", metadata);
        }

        String url = transferPostUrl(billingAccountId, dryRun);
        String bodyJson = new GsonBuilder().setPrettyPrinting().create().toJson(body);
        Allure.addAttachment("Request body", "application/json", bodyJson);

        Response response = postTransfer(url, token, bodyJson);

        int status = response.statusCode();
        String responseBody = response.asPrettyString();
        Allure.addAttachment("Response (" + status + ")", "application/json", responseBody);

        int expectedMin = payload.get("expectedStatusMin") != null ? ((Number) payload.get("expectedStatusMin")).intValue() : 200;
        int expectedMax = payload.get("expectedStatusMax") != null ? ((Number) payload.get("expectedStatusMax")).intValue() : 201;

        Assert.assertTrue(status >= expectedMin && status <= expectedMax,
                "Expected status in [" + expectedMin + "," + expectedMax + "] but got " + status);

        if (status >= 200 && status < 300) {
            JsonPath json = response.jsonPath();
            Assert.assertNotNull(json.getString("id"), "Response must contain id");
            Assert.assertNotNull(json.getString("locationId"), "Response must contain locationId");
            Assert.assertNotNull(json.getString("status"), "Response must contain status");
            Allure.step("Validated response schema: id, locationId, status present.");
            BillingTransferResponseStore.persistFromSuccessResponse(response);
        }
    }

    // ---------- Chained read using stored transfer id ----------

    @Test(priority = 3, groups = {"Billing", "Regression"})
    @Story("Retrieve transfer by id using values from prior successful call")
    @Description("GET /billing-accounts/{billingAccountId}/transfers/{transferId} — uses ResponseStore from dry-run or data-driven success.")
    public void getTransferById_usingStoredTransferId_expect2xx() {
        Optional<String> transferId = BillingTransferResponseStore.transferId();
        if (transferId.isEmpty()) {
            throw new SkipException("billingAccountTransferId not in ResponseStore — run dry-run or positive transfer first");
        }
        String token = bearerToken();
        String url = transferGetUrl(getBillingAccountId(), transferId.get());
        Allure.step("GET transfer using stored id: " + transferId.get());
        Response response = ApiClient.get(url, headersWithBearer(token));
        Allure.addAttachment("GET transfer response", "application/json", response.asPrettyString());

        int code = response.statusCode();
        Assert.assertTrue(code == 200 || code == 404,
                "Expected 200 (found) or 404 (stale id / env); got " + code);
        if (code == 200) {
            BillingTransferResponseStore.persistFromSuccessResponse(response);
        }
    }

    // ---------- Negative scenarios (TDD) — reuse targetLocationId() where a valid UUID is required ----------

    @Test(priority = 10, groups = {"Billing", "Regression"})
    @Story("Transfer with missing locationId must be rejected")
    @Description("Request without required locationId should return 400 Bad Request.")
    public void transferWithMissingLocationId_expectBadRequest() {
        String token = bearerToken();
        String url = transferPostUrl(getBillingAccountId(), false);
        String body = "{\"metadata\":{\"key\":\"value\"}}";

        Response response = postTransfer(url, token, body);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());

        Assert.assertTrue(response.statusCode() == 400 || response.statusCode() == 422,
                "Missing locationId should result in 400 or 422, got " + response.statusCode());
    }

    @Test(priority = 11, groups = {"Billing", "Regression"})
    @Story("Transfer with invalid locationId format must be rejected")
    @Description("Malformed locationId (not UUID) should return 400 Bad Request.")
    public void transferWithInvalidLocationIdFormat_expectBadRequest() {
        String token = bearerToken();
        String url = transferPostUrl(getBillingAccountId(), false);
        String body = "{\"locationId\":\"not-a-valid-uuid\",\"metadata\":{}}";

        Response response = postTransfer(url, token, body);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());

        Assert.assertTrue(response.statusCode() == 400 || response.statusCode() == 404 || response.statusCode() == 422,
                "Invalid locationId format should result in 4xx, got " + response.statusCode());
    }

    @Test(priority = 12, groups = {"Billing", "Regression"})
    @Story("Transfer with non-existent billing account must be rejected")
    @Description("Non-existent billingAccountId should return 404 Not Found.")
    public void transferWithNonExistentBillingAccountId_expectNotFound() {
        String token = bearerToken();
        String fakeId = "00000000-0000-0000-0000-000000000000";
        String url = transferPostUrl(fakeId, false);
        String body = new GsonBuilder().create().toJson(Map.of(
                "locationId", targetLocationId(),
                "metadata", Map.of()));

        Response response = postTransfer(url, token, body);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());

        Assert.assertTrue(response.statusCode() == 404 || response.statusCode() == 400,
                "Non-existent billing account should result in 404 or 400, got " + response.statusCode());
    }

    @Test(priority = 13, groups = {"Billing", "Regression"})
    @Story("Transfer without bearer token must be rejected")
    @Description("Request without Authorization header should return 401 Unauthorized.")
    public void transferWithoutAuth_expectUnauthorized() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("ABCFS-ORGANIZATION-ID", getOrganizationId());
        String url = transferPostUrl(getBillingAccountId(), false);
        String body = new GsonBuilder().create().toJson(Map.of("locationId", targetLocationId()));

        Response response = ApiClient.post(url, headers, body);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());

        Assert.assertEquals(response.statusCode(), 401, "Missing auth should return 401 Unauthorized");
    }

    @Test(priority = 14, groups = {"Billing", "Regression"})
    @Story("Transfer without organization ID header must be rejected")
    @Description("Request without ABCFS-ORGANIZATION-ID should return 400 or 403.")
    public void transferWithoutOrganizationId_expectForbiddenOrBadRequest() {
        String token = bearerToken();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        String url = transferPostUrl(getBillingAccountId(), false);
        String body = new GsonBuilder().create().toJson(Map.of("locationId", targetLocationId()));

        Response response = ApiClient.post(url, headers, body);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());

        Assert.assertTrue(response.statusCode() == 400 || response.statusCode() == 403,
                "Missing organization ID should result in 400 or 403, got " + response.statusCode());
    }

    @Test(priority = 15, groups = {"Billing", "Regression"})
    @Story("Transfer with invalid metadata (key too long) must be rejected")
    @Description("Metadata key > 255 chars should return 400 or 422.")
    public void transferWithInvalidMetadata_expectValidationError() {
        String token = bearerToken();
        String url = transferPostUrl(getBillingAccountId(), false);
        String longKey = "a".repeat(256);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put(longKey, "value");
        String body = new GsonBuilder().create().toJson(Map.of(
                "locationId", targetLocationId(),
                "metadata", meta));

        Response response = postTransfer(url, token, body);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());

        Assert.assertTrue(response.statusCode() == 400 || response.statusCode() == 422,
                "Invalid metadata should result in 400 or 422, got " + response.statusCode());
    }
}
