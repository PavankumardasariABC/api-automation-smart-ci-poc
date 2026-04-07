package com.ordersession.tests;

import com.google.gson.GsonBuilder;
import com.ordersession.auth.OrderSessionAuth;
import com.ordersession.config.ApiClient;
import com.ordersession.config.ConfigManager;
import com.ordersession.dataprovider.OrderSessionApiTemplateDataProvider;
import com.ordersession.dataprovider.OrderSessionDataProvider;
import com.ordersession.store.ResponseStore;
import com.ordersession.support.ErrorPayloadAssertions;
import com.ordersession.support.OrderSessionTestConfig;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Order Session API — E2E suite structured like {@code CreateBillingAccountTransferTests} (root POC).
 * Covers all operations from {@code openapi-spec-5.yaml}:
 * <ul>
 *   <li>{@code POST /payment-session/tokens} — createPaymentTokenSession</li>
 *   <li>{@code GET /payment-session/tokens/{id}} — getPaymentTokenSession</li>
 *   <li>{@code POST /payment-session/wallet-entries} — createWalletEntrySession</li>
 *   <li>{@code GET /payment-session/wallet-entries/{id}} — getWalletEntrySession</li>
 * </ul>
 */
@Epic("Order Session API")
@Feature("Order Session — E2E (billing template style)")
@Severity(SeverityLevel.BLOCKER)
public class OrderSessionApiE2ETests {

    private static final String PATH_PAYMENT_TOKENS = "/payment-session/tokens";
    private static final String PATH_WALLET_ENTRIES = "/payment-session/wallet-entries";

    public static final String STORE_PAYMENT_SESSION_ID = "OrderSessionLastPaymentSessionId";
    public static final String STORE_WALLET_SESSION_ID = "OrderSessionLastWalletEntrySessionId";

    private static String baseUrl() {
        return ConfigManager.get("order.session.base.url").replaceAll("/$", "");
    }

    private static String urlPaymentTokens() {
        return baseUrl() + PATH_PAYMENT_TOKENS;
    }

    private static String urlPaymentTokenById(String id) {
        return baseUrl() + PATH_PAYMENT_TOKENS + "/" + id;
    }

    private static String urlWalletEntries() {
        return baseUrl() + PATH_WALLET_ENTRIES;
    }

    private static String urlWalletEntryById(String id) {
        return baseUrl() + PATH_WALLET_ENTRIES + "/" + id;
    }

    /** Tenant header: only {@link OrderSessionTestConfig#organizationId()} (config / *.local.properties). */
    private static Map<String, String> headersWithBearer(String token) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("ABCFS-ORGANIZATION-ID", OrderSessionTestConfig.organizationId());
        headers.put("User-Agent", "OrderSession-Automation/1.0");
        return headers;
    }

    private static String bearer() {
        return OrderSessionAuth.bearerToken();
    }

    // -------------------------------------------------------------------------
    // Positive / data-driven (OpenAPI happy paths)
    // -------------------------------------------------------------------------

    @Story("Create token payment session — data-driven scenarios")
    @TmsLink("createPaymentTokenSession")
    @Test(
            dataProvider = "orderSessionPaymentTokenCreateData",
            dataProviderClass = OrderSessionApiTemplateDataProvider.class,
            priority = 1,
            groups = {"OrderSession", "Regression", "Sanity"}
    )
    @Description("POST /payment-session/tokens — JSON scenarios; requires test.consumer.id + test.location.id.")
    public void createPaymentTokenSession_expectedSuccess(Map<String, Object> scenario) {
        if (!OrderSessionTestConfig.isPaymentTokenFlowReady()) {
            throw new SkipException("Set test.consumer.id and test.location.id (and valid token) for integration");
        }

        String ownerType = String.valueOf(scenario.get("ownerType"));
        Allure.step("Scenario: " + scenario.get("scenario") + " — " + scenario.get("description"));

        Map<String, Object> body = OrderSessionDataProvider.validPaymentTokenBody(ownerType);
        String bodyJson = new GsonBuilder().setPrettyPrinting().create().toJson(body);
        Allure.addAttachment("Request body", "application/json", bodyJson);

        Response response = ApiClient.post(urlPaymentTokens(), headersWithBearer(bearer()), bodyJson);
        if (response.statusCode() == 401) {
            response = ApiClient.post(urlPaymentTokens(), headersWithBearer(bearer()), bodyJson);
        }

        int status = response.statusCode();
        Allure.addAttachment("Response (" + status + ")", "application/json", response.asPrettyString());

        int min = scenario.get("expectedStatusMin") != null
                ? ((Number) scenario.get("expectedStatusMin")).intValue() : 201;
        int max = scenario.get("expectedStatusMax") != null
                ? ((Number) scenario.get("expectedStatusMax")).intValue() : 201;
        Assert.assertTrue(status >= min && status <= max,
                "Expected HTTP in [" + min + "," + max + "] but got " + status);

        if (status == 201) {
            JsonPath jp = response.jsonPath();
            Assert.assertNotNull(jp.getString("id"), "Response must contain id");
            Assert.assertNotNull(jp.getString("status"), "Response must contain status");
            Assert.assertNotNull(jp.getString("url"), "Response must contain hosted url");
            ResponseStore.put(STORE_PAYMENT_SESSION_ID, jp.getString("id"));
            Allure.step("Stored " + STORE_PAYMENT_SESSION_ID + " for GET follow-up");
        }
    }

    @Story("Create wallet entry session — data-driven scenarios")
    @TmsLink("createWalletEntrySession")
    @Test(
            dataProvider = "orderSessionWalletEntryCreateData",
            dataProviderClass = OrderSessionApiTemplateDataProvider.class,
            priority = 2,
            groups = {"OrderSession", "Regression", "Sanity"}
    )
    @Description("POST /payment-session/wallet-entries — JSON scenarios; requires full wallet test.* IDs.")
    public void createWalletEntrySession_expectedSuccess(Map<String, Object> scenario) {
        if (!OrderSessionTestConfig.isWalletEntryFlowReady()) {
            throw new SkipException("Set wallet + payment token test.* IDs for integration");
        }

        Allure.step("Scenario: " + scenario.get("scenario") + " — " + scenario.get("description"));

        Map<String, Object> body = OrderSessionDataProvider.validWalletEntryBody();
        if (!Boolean.TRUE.equals(scenario.get("includePaymentMethods"))) {
            body.remove("paymentMethods");
        }
        if (!Boolean.TRUE.equals(scenario.get("includeSupportedTags"))) {
            body.remove("supportedTags");
        }
        if (!Boolean.TRUE.equals(scenario.get("includeMetadata"))) {
            body.remove("metadata");
        }

        String bodyJson = new GsonBuilder().setPrettyPrinting().create().toJson(body);
        Allure.addAttachment("Request body", "application/json", bodyJson);

        Response response = ApiClient.post(urlWalletEntries(), headersWithBearer(bearer()), bodyJson);
        if (response.statusCode() == 401) {
            response = ApiClient.post(urlWalletEntries(), headersWithBearer(bearer()), bodyJson);
        }

        int status = response.statusCode();
        Allure.addAttachment("Response (" + status + ")", "application/json", response.asPrettyString());

        int min = scenario.get("expectedStatusMin") != null
                ? ((Number) scenario.get("expectedStatusMin")).intValue() : 201;
        int max = scenario.get("expectedStatusMax") != null
                ? ((Number) scenario.get("expectedStatusMax")).intValue() : 201;
        Assert.assertTrue(status >= min && status <= max,
                "Expected HTTP in [" + min + "," + max + "] but got " + status);

        if (status == 201) {
            JsonPath jp = response.jsonPath();
            Assert.assertNotNull(jp.getString("id"), "Response must contain id");
            Assert.assertNotNull(jp.getString("url"), "POST must return hosted url");
            ResponseStore.put(STORE_WALLET_SESSION_ID, jp.getString("id"));
        }
    }

    @Story("GET payment token session by id after create")
    @TmsLink("getPaymentTokenSession")
    @Test(priority = 5, groups = {"OrderSession", "Regression", "Sanity"})
    @Description("GET /payment-session/tokens/{id} — uses last session id from successful POST.")
    public void getPaymentTokenSession_validId_expectedSuccess() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Bearer token required");
        }
        String id = Optional.ofNullable(ResponseStore.get(STORE_PAYMENT_SESSION_ID))
                .map(Object::toString)
                .filter(s -> !s.isBlank())
                .orElse(null);
        if (id == null) {
            throw new SkipException("No session id in store; run create payment token tests first");
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + bearer());
        headers.put("ABCFS-ORGANIZATION-ID", OrderSessionTestConfig.organizationId());
        headers.put("User-Agent", "OrderSession-Automation/1.0");

        Response response = ApiClient.get(urlPaymentTokenById(id), headers);
        if (response.statusCode() == 401) {
            response = ApiClient.get(urlPaymentTokenById(id), headers);
        }
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        Assert.assertEquals(response.statusCode(), 200, response.asPrettyString());
        JsonPath jp = response.jsonPath();
        Assert.assertEquals(jp.getString("id"), id);
        String st = jp.getString("status");
        Assert.assertNotNull(st, "status");
    }

    @Story("GET wallet entry session — url omitted on GET per OpenAPI")
    @TmsLink("getWalletEntrySession")
    @Test(priority = 6, groups = {"OrderSession", "Regression", "Sanity"})
    @Description("GET /payment-session/wallet-entries/{id} — assert 200; url should not be present.")
    public void getWalletEntrySession_validId_expectedSuccess() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Bearer token required");
        }
        String id = Optional.ofNullable(ResponseStore.get(STORE_WALLET_SESSION_ID))
                .map(Object::toString)
                .filter(s -> !s.isBlank())
                .orElse(null);
        if (id == null) {
            throw new SkipException("No wallet session id in store; run wallet create tests first");
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + bearer());
        headers.put("ABCFS-ORGANIZATION-ID", OrderSessionTestConfig.organizationId());
        headers.put("User-Agent", "OrderSession-Automation/1.0");

        Response response = ApiClient.get(urlWalletEntryById(id), headers);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        Assert.assertEquals(response.statusCode(), 200, response.asPrettyString());
        JsonPath jp = response.jsonPath();
        Assert.assertEquals(jp.getString("id"), id);
        Object url = jp.get("url");
        Assert.assertTrue(url == null || url.toString().isEmpty(),
                "OpenAPI: url only on POST create, not on GET");
    }

    // -------------------------------------------------------------------------
    // Negative / validation (TDD style)
    // -------------------------------------------------------------------------

    @Test(priority = 10, groups = {"OrderSession", "Regression"})
    @Story("GET payment token — unknown id returns 404")
    @Description("GET /payment-session/tokens/{id} with random UUID — NotFound per spec.")
    public void getPaymentTokenSession_unknownId_expectNotFound() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Bearer required");
        }
        if (!OrderSessionTestConfig.hasRealOrganizationId()) {
            throw new SkipException("Real abcfs.organization.id required (placeholder yields 401)");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + bearer());
        headers.put("ABCFS-ORGANIZATION-ID", OrderSessionTestConfig.organizationId());
        headers.put("User-Agent", "OrderSession-Automation/1.0");

        Response response = ApiClient.get(urlPaymentTokenById(UUID.randomUUID().toString()), headers);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        Assert.assertEquals(response.statusCode(), 404, response.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(response);
    }

    @Test(priority = 11, groups = {"OrderSession", "Regression"})
    @Story("GET wallet entry — unknown id returns 404")
    public void getWalletEntrySession_unknownId_expectNotFound() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Bearer required");
        }
        if (!OrderSessionTestConfig.hasRealOrganizationId()) {
            throw new SkipException("Real abcfs.organization.id required");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + bearer());
        headers.put("ABCFS-ORGANIZATION-ID", OrderSessionTestConfig.organizationId());
        headers.put("User-Agent", "OrderSession-Automation/1.0");

        Response response = ApiClient.get(urlWalletEntryById(UUID.randomUUID().toString()), headers);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        Assert.assertEquals(response.statusCode(), 404, response.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(response);
    }

    @Test(priority = 12, groups = {"OrderSession", "Regression"})
    @Story("Create payment token — missing consumerId")
    public void createPaymentTokenSession_missingConsumerId_expectBadRequest() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Bearer required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ownerType", "PAYOR");
        body.put("locationId", OrderSessionDataProvider.sampleLocationId());
        String json = new GsonBuilder().create().toJson(body);

        Response response = ApiClient.post(urlPaymentTokens(), headersWithBearer(bearer()), json);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        Assert.assertEquals(response.statusCode(), 400, response.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(response);
    }

    @Test(priority = 13, groups = {"OrderSession", "Regression"})
    @Story("Create payment token — missing ownerType")
    public void createPaymentTokenSession_missingOwnerType_expectBadRequest() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Bearer required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("consumerId", OrderSessionDataProvider.sampleConsumerId());
        body.put("locationId", OrderSessionDataProvider.sampleLocationId());
        String json = new GsonBuilder().create().toJson(body);

        Response response = ApiClient.post(urlPaymentTokens(), headersWithBearer(bearer()), json);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        Assert.assertEquals(response.statusCode(), 400, response.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(response);
    }

    @Test(priority = 14, groups = {"OrderSession", "Regression"})
    @Story("Create payment token — missing locationId")
    public void createPaymentTokenSession_missingLocationId_expectBadRequest() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Bearer required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("consumerId", OrderSessionDataProvider.sampleConsumerId());
        body.put("ownerType", "PAYOR");
        String json = new GsonBuilder().create().toJson(body);

        Response response = ApiClient.post(urlPaymentTokens(), headersWithBearer(bearer()), json);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        Assert.assertEquals(response.statusCode(), 400, response.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(response);
    }

    @Test(priority = 15, groups = {"OrderSession", "Regression"})
    @Story("Create payment token — invalid consumerId format")
    public void createPaymentTokenSession_invalidConsumerId_expectBadRequest() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Bearer required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("consumerId", "not-a-uuid");
        body.put("ownerType", "PAYOR");
        body.put("locationId", OrderSessionDataProvider.sampleLocationId());
        String json = new GsonBuilder().create().toJson(body);

        Response response = ApiClient.post(urlPaymentTokens(), headersWithBearer(bearer()), json);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        Assert.assertTrue(response.statusCode() == 400 || response.statusCode() == 422,
                "Expected 400/422, got " + response.statusCode());
        ErrorPayloadAssertions.assertErrorPayloadShape(response);
    }

    @Test(priority = 16, groups = {"OrderSession", "Regression"})
    @Story("Create wallet entry — missing ownerId")
    public void createWalletEntrySession_missingOwnerId_expectBadRequest() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Bearer required");
        }
        Map<String, Object> body = OrderSessionDataProvider.validWalletEntryBody();
        body.remove("ownerId");
        String json = new GsonBuilder().create().toJson(body);

        Response response = ApiClient.post(urlWalletEntries(), headersWithBearer(bearer()), json);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        Assert.assertEquals(response.statusCode(), 400, response.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(response);
    }

    @Test(priority = 17, groups = {"OrderSession", "Regression"})
    @Story("Create wallet entry — missing user object")
    public void createWalletEntrySession_missingUser_expectBadRequest() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Bearer required");
        }
        Map<String, Object> body = OrderSessionDataProvider.validWalletEntryBody();
        body.remove("user");
        String json = new GsonBuilder().create().toJson(body);

        Response response = ApiClient.post(urlWalletEntries(), headersWithBearer(bearer()), json);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        Assert.assertEquals(response.statusCode(), 400, response.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(response);
    }

    @Test(priority = 20, groups = {"OrderSession", "Regression", "Smoke"})
    @Story("POST payment token — without bearer")
    public void createPaymentTokenSession_withoutAuth_expectUnauthorized() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("ABCFS-ORGANIZATION-ID", OrderSessionTestConfig.organizationId());
        String json = new GsonBuilder().create().toJson(OrderSessionDataProvider.validPaymentTokenBody("PAYOR"));

        Response response = ApiClient.post(urlPaymentTokens(), headers, json);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        Assert.assertEquals(response.statusCode(), 401, response.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(response);
    }

    @Test(priority = 21, groups = {"OrderSession", "Regression", "Smoke"})
    @Story("POST wallet entry — without bearer")
    public void createWalletEntrySession_withoutAuth_expectUnauthorized() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("ABCFS-ORGANIZATION-ID", OrderSessionTestConfig.organizationId());
        String json = new GsonBuilder().create().toJson(OrderSessionDataProvider.validWalletEntryBody());

        Response response = ApiClient.post(urlWalletEntries(), headers, json);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        Assert.assertEquals(response.statusCode(), 401, response.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(response);
    }

    @Test(priority = 22, groups = {"OrderSession", "Regression", "Smoke"})
    @Story("GET payment token — without bearer")
    public void getPaymentTokenSession_withoutAuth_expectUnauthorized() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("ABCFS-ORGANIZATION-ID", OrderSessionTestConfig.organizationId());
        Response response = ApiClient.get(urlPaymentTokenById(UUID.randomUUID().toString()), headers);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        Assert.assertEquals(response.statusCode(), 401, response.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(response);
    }

    @Test(priority = 23, groups = {"OrderSession", "Regression", "Smoke"})
    @Story("GET wallet entry — without bearer")
    public void getWalletEntrySession_withoutAuth_expectUnauthorized() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("ABCFS-ORGANIZATION-ID", OrderSessionTestConfig.organizationId());
        Response response = ApiClient.get(urlWalletEntryById(UUID.randomUUID().toString()), headers);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        Assert.assertEquals(response.statusCode(), 401, response.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(response);
    }

    @Test(priority = 24, groups = {"OrderSession", "Regression"})
    @Story("POST payment token — without organization header")
    @Description("Gateway may return 401, 403, or 400 — assert non-success auth context.")
    public void createPaymentTokenSession_withoutOrganization_expectRejected() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Bearer required");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + bearer());
        headers.put("Content-Type", "application/json;charset=UTF-8");
        String json = new GsonBuilder().create().toJson(OrderSessionDataProvider.validPaymentTokenBody("PAYOR"));

        Response response = ApiClient.post(urlPaymentTokens(), headers, json);
        Allure.addAttachment("Response", "application/json", response.asPrettyString());
        int code = response.statusCode();
        Assert.assertTrue(code == 400 || code == 401 || code == 403,
                "Missing ABCFS-ORGANIZATION-ID should be rejected, got " + code);
    }
}
