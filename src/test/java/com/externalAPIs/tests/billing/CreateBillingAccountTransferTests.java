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
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Billing Account Transfer API – Template Test Suite (TDD).
 * <p>
 * API: POST /billing-accounts/{billingAccountId}/transfer
 * Initiates transfer of billing account to a new location. Supports dryRun for eligibility check.
 * </p>
 * <p>
 * Reusable template: Update BILLING_TRANSFER_PATH and test data for your environment.
 * </p>
 */
@Epic("Billing")
@Feature("Billing Account Transfer API")
@Severity(SeverityLevel.CRITICAL)
public class CreateBillingAccountTransferTests {

    /** Path suffix for transfer endpoint. Change for your API version. */
    private static final String BILLING_TRANSFER_PATH = "/api/abcpg/billing-accounts/%s/transfer";

    /** Default billing account UUID for demo; override with -Dbilling.account.id=uuid or env property. */
    private static final String DEFAULT_BILLING_ACCOUNT_ID = "6f1e19b3-1ed2-44a2-8de9-3e2c4c2db5e6";

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

    private static Map<String, String> headersWithBearer(String token) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("ABCFS-ORGANIZATION-ID", getOrganizationId());
        headers.put("User-Agent", "Automation-Test");
        return headers;
    }

    // ---------- Positive / data-driven scenarios ----------

    @Story("As an admin, I can transfer a billing account to a new location")
    @Test(dataProvider = "billingAccountTransferData", dataProviderClass = BillingDataProvider.class, priority = 1, groups = {"Billing", "Regression"})
    @Description("Data-driven: valid transfer (execute and dry-run) with expected 2xx response and schema validation.")
    public void transferWithValidPayload_expectedSuccess(Map<String, Object> payload) {
        String token = AuthUtils.getSecureClientToken();
        Assert.assertNotNull(token, "Secure client token is required");

        String billingAccountId = getBillingAccountId();
        Boolean dryRun = payload.get("dryRun") != null ? Boolean.TRUE.equals(payload.get("dryRun")) : false;
        String locationId = String.valueOf(payload.get("locationId"));
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = payload.containsKey("metadata") && payload.get("metadata") != null
                ? (Map<String, Object>) payload.get("metadata") : Map.of();

        Allure.step("Scenario: " + payload.get("scenario") + " – " + payload.get("description"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("locationId", locationId);
        if (!metadata.isEmpty()) body.put("metadata", metadata);

        String baseUrl = ConfigManager.getSecureBaseUrl();
        String path = String.format(BILLING_TRANSFER_PATH, billingAccountId);
        String url = baseUrl + path + (dryRun ? "?dryRun=true" : "");

        String bodyJson = new GsonBuilder().setPrettyPrinting().create().toJson(body);
        Allure.addAttachment("Request body", "application/json", bodyJson);

        Response response = ApiClient.post(url, headersWithBearer(token), bodyJson);
        if (response.statusCode() == 401) {
            token = AuthUtils.getSecureClientToken();
            response = ApiClient.post(url, headersWithBearer(token), bodyJson);
        }

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
        }
    }

    // ---------- Negative scenarios (TDD) ----------

    @Test(priority = 2, groups = {"Billing", "Regression"})
    @Story("Transfer with missing locationId must be rejected")
    @Description("Request without required locationId should return 400 Bad Request.")
    public void transferWithMissingLocationId_expectBadRequest() {
        String token = AuthUtils.getSecureClientToken();
        String url = ConfigManager.getSecureBaseUrl() + String.format(BILLING_TRANSFER_PATH, getBillingAccountId());
        String body = "{\"metadata\":{\"key\":\"value\"}}"; // no locationId

        Response response = ApiClient.post(url, headersWithBearer(token), body);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());

        Assert.assertTrue(response.statusCode() == 400 || response.statusCode() == 422,
                "Missing locationId should result in 400 or 422, got " + response.statusCode());
    }

    @Test(priority = 3, groups = {"Billing", "Regression"})
    @Story("Transfer with invalid locationId format must be rejected")
    @Description("Malformed locationId (not UUID) should return 400 Bad Request.")
    public void transferWithInvalidLocationIdFormat_expectBadRequest() {
        String token = AuthUtils.getSecureClientToken();
        String url = ConfigManager.getSecureBaseUrl() + String.format(BILLING_TRANSFER_PATH, getBillingAccountId());
        String body = "{\"locationId\":\"not-a-valid-uuid\",\"metadata\":{}}";

        Response response = ApiClient.post(url, headersWithBearer(token), body);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());

        Assert.assertTrue(response.statusCode() == 400 || response.statusCode() == 404 || response.statusCode() == 422,
                "Invalid locationId format should result in 4xx, got " + response.statusCode());
    }

    @Test(priority = 4, groups = {"Billing", "Regression"})
    @Story("Transfer with non-existent billing account must be rejected")
    @Description("Non-existent billingAccountId should return 404 Not Found.")
    public void transferWithNonExistentBillingAccountId_expectNotFound() {
        String token = AuthUtils.getSecureClientToken();
        String fakeId = "00000000-0000-0000-0000-000000000000";
        String url = ConfigManager.getSecureBaseUrl() + String.format(BILLING_TRANSFER_PATH, fakeId);
        String body = "{\"locationId\":\"11ec0af2-3a19-b7d3-a84f-59243ef7e239\",\"metadata\":{}}";

        Response response = ApiClient.post(url, headersWithBearer(token), body);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());

        Assert.assertTrue(response.statusCode() == 404 || response.statusCode() == 400,
                "Non-existent billing account should result in 404 or 400, got " + response.statusCode());
    }

    @Test(priority = 5, groups = {"Billing", "Regression"})
    @Story("Transfer without bearer token must be rejected")
    @Description("Request without Authorization header should return 401 Unauthorized.")
    public void transferWithoutAuth_expectUnauthorized() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("ABCFS-ORGANIZATION-ID", getOrganizationId());
        String url = ConfigManager.getSecureBaseUrl() + String.format(BILLING_TRANSFER_PATH, getBillingAccountId());
        String body = "{\"locationId\":\"11ec0af2-3a19-b7d3-a84f-59243ef7e239\"}";

        Response response = ApiClient.post(url, headers, body);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());

        Assert.assertEquals(response.statusCode(), 401, "Missing auth should return 401 Unauthorized");
    }

    @Test(priority = 6, groups = {"Billing", "Regression"})
    @Story("Transfer without organization ID header must be rejected")
    @Description("Request without ABCFS-ORGANIZATION-ID should return 400 or 403.")
    public void transferWithoutOrganizationId_expectForbiddenOrBadRequest() {
        String token = AuthUtils.getSecureClientToken();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        // omit ABCFS-ORGANIZATION-ID
        String url = ConfigManager.getSecureBaseUrl() + String.format(BILLING_TRANSFER_PATH, getBillingAccountId());
        String body = "{\"locationId\":\"11ec0af2-3a19-b7d3-a84f-59243ef7e239\"}";

        Response response = ApiClient.post(url, headers, body);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());

        Assert.assertTrue(response.statusCode() == 400 || response.statusCode() == 403,
                "Missing organization ID should result in 400 or 403, got " + response.statusCode());
    }

    @Test(priority = 7, groups = {"Billing", "Regression"})
    @Story("Transfer with invalid metadata (key too long) must be rejected")
    @Description("Metadata key > 255 chars should return 400 or 422.")
    public void transferWithInvalidMetadata_expectValidationError() {
        String token = AuthUtils.getSecureClientToken();
        String url = ConfigManager.getSecureBaseUrl() + String.format(BILLING_TRANSFER_PATH, getBillingAccountId());
        String longKey = "a".repeat(256);
        String body = "{\"locationId\":\"11ec0af2-3a19-b7d3-a84f-59243ef7e239\",\"metadata\":{\"" + longKey + "\":\"value\"}}";

        Response response = ApiClient.post(url, headersWithBearer(token), body);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());

        Assert.assertTrue(response.statusCode() == 400 || response.statusCode() == 422,
                "Invalid metadata should result in 400 or 422, got " + response.statusCode());
    }

    @Test(priority = 8, groups = {"Billing", "Regression"})
    @Story("Dry run returns eligibility status without executing transfer")
    @Description("dryRun=true should return 2xx with status (e.g. ELIGIBLE) without persisting transfer.")
    public void dryRunTrue_returnsEligibilityStatus() {
        String token = AuthUtils.getSecureClientToken();
        String url = ConfigManager.getSecureBaseUrl() + String.format(BILLING_TRANSFER_PATH, getBillingAccountId()) + "?dryRun=true";
        String body = "{\"locationId\":\"11ec0af2-3a19-b7d3-a84f-59243ef7e239\",\"metadata\":{\"reason\":\"EligibilityCheck\"}}";

        Response response = ApiClient.post(url, headersWithBearer(token), body);
        String responseBody = response.asPrettyString();
        Allure.addAttachment("Response", "application/json", responseBody);

        Assert.assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
                "Dry run should return 2xx, got " + response.statusCode());
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            String status = response.jsonPath().getString("status");
            Allure.step("Transfer eligibility status: " + status);
        }
    }
}
