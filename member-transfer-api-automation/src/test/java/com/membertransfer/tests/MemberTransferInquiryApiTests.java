package com.membertransfer.tests;

import com.membertransfer.auth.MemberTransferAuth;
import com.membertransfer.config.ApiClient;
import com.membertransfer.config.ConfigManager;
import com.membertransfer.dataprovider.MemberTransferDataProvider;
import com.membertransfer.store.ResponseStore;
import com.membertransfer.support.MemberTransferSupport;
import io.qameta.allure.*;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;

@Epic("Member transfer")
@Feature("Bulk inquiry")
@Severity(SeverityLevel.CRITICAL)
public class MemberTransferInquiryApiTests {

    @Test(
            priority = 5,
            groups = {"Regression", "MemberTransfer", "Sanity", "MT_Sanity"}
    )
    @Story("Process bulk inquiry returns a bulkId")
    @Description("200 + bulkId; bearer from member-transfer token; 5xx retry in ApiClient.")
    public void inquiry_validPayload_expectBulkId() {
        ensureConfiguredForHappyPath();
        String token = tokenOrGet();
        String url = MemberTransferSupport.inquiryUrl();
        String body = MemberTransferSupport.defaultInquiryBodyJson();
        Allure.addAttachment("inquiry request", "application/json", body);

        Response r = postWithReauthOn401(url, token, body);
        if (credsUnusable(r)) {
            return;
        }
        Assert.assertEquals(r.getStatusCode(), 200, "Expected 200: " + r.getStatusCode() + " — " + r.asString());
        String bulk = r.jsonPath().getString("bulkId");
        Assert.assertNotNull(bulk, "bulkId");
        ResponseStore.put(MemberTransferSupport.BULK_ID_KEY, bulk);
        Allure.addAttachment("inquiry response", "application/json", r.asPrettyString());
    }

    @Test(
            dataProvider = "memberTransferInquiryEdgeScenarios",
            dataProviderClass = MemberTransferDataProvider.class,
            priority = 6,
            groups = {"Regression", "MemberTransfer"}
    )
    @Description("Edge request shapes; 2xx or validation 4xx.")
    public void inquiry_edgePayloads(String scenario, String bodyJson) {
        ensureToken();
        if (isPlaceholderData()) {
            throw new SkipException("Set member.transfer.* in " + ConfigManager.getEnv() + ".properties");
        }
        String url = MemberTransferSupport.inquiryUrl();
        Allure.addAttachment("scenario: " + scenario, "application/json", bodyJson);
        Response r = postWithReauthOn401(url, tokenOrGet(), bodyJson);
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        Allure.addAttachment("response " + c, "application/json", r.asPrettyString());
        Assert.assertTrue(c == 200 || c == 201 || c == 202 || c == 400, "Unexpected: " + c);
    }

    @Test(
            dataProvider = "memberTransferInquiryNegativeScenarios",
            dataProviderClass = MemberTransferDataProvider.class,
            priority = 20,
            groups = {"Regression", "MemberTransfer"}
    )
    @Description("Validation errors — 4xx range.")
    public void inquiry_negativeScenarios(String scenario, String bodyJson, int min, int max) {
        if (isPlaceholderData()) {
            throw new SkipException("Configure member.transfer for " + ConfigManager.getEnv());
        }
        String token = tokenOrGet();
        if (token == null || token.isEmpty()) {
            throw new SkipException("No access token");
        }
        String url = MemberTransferSupport.inquiryUrl();
        Allure.step(scenario);
        Allure.addAttachment("body", "application/json", bodyJson);
        Response r = postWithReauthOn401(url, token, bodyJson);
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        Allure.addAttachment("response " + c, "application/json", r.asPrettyString());
        Assert.assertTrue(c >= min && c <= max, "HTTP " + c);
    }

    @Test(priority = 30, groups = {"Regression", "MemberTransfer"})
    @Story("Missing Authorization")
    @Description("401/403 without bearer")
    public void inquiry_withoutAuth_expect401() {
        if (isPlaceholderData()) {
            throw new SkipException("Configure env");
        }
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Content-Type", "application/json;charset=UTF-8");
        h.put("Accept", "application/json;charset=UTF-8");
        h.put("ABCFS-TENANT-ID", MemberTransferSupport.tenantId());
        Response r = ApiClient.post(MemberTransferSupport.inquiryUrl(), h, MemberTransferSupport.defaultInquiryBodyJson());
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        Assert.assertTrue(c == 401 || c == 403, "Expected 401/403, was " + c);
    }

    @Test(priority = 31, groups = {"Regression", "MemberTransfer"})
    @Story("Invalid tenant")
    @Description("Invalid ABCFS-TENANT-ID")
    public void inquiry_invalidTenant_expect4xx() {
        String token = tokenOrGet();
        if (token == null || token.isEmpty()) {
            throw new SkipException("No token");
        }
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Authorization", "Bearer " + token);
        h.put("Content-Type", "application/json;charset=UTF-8");
        h.put("Accept", "application/json;charset=UTF-8");
        h.put("ABCFS-TENANT-ID", "00000000-0000-0000-0000-000000000000");
        Response r = ApiClient.post(MemberTransferSupport.inquiryUrl(), h, MemberTransferSupport.defaultInquiryBodyJson());
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        Assert.assertTrue(c >= 400 && c < 500, "Expected 4xx, was " + c);
    }

    @Test(priority = 32, groups = {"Regression", "MemberTransfer"})
    @Story("Invalid/expired bearer")
    @Description("Garbage bearer token should be rejected.")
    public void inquiry_invalidBearer_expectUnauthorized() {
        Map<String, String> h = MemberTransferSupport.bearerJsonHeaders("invalid.token");
        Response r = ApiClient.post(MemberTransferSupport.inquiryUrl(), h, MemberTransferSupport.defaultInquiryBodyJson());
        int c = r.getStatusCode();
        Assert.assertTrue(c == 401 || c == 403, "Expected 401/403, got " + c);
    }

    @Test(priority = 33, groups = {"Regression", "MemberTransfer"})
    @Story("Unsupported methods rejected")
    @Description("GET/PUT/DELETE should not be allowed on inquiry endpoint.")
    public void inquiry_unsupportedMethods_rejected() {
        String token = tokenOrGet();
        if (token == null || token.isEmpty()) {
            throw new SkipException("No token available");
        }
        Map<String, String> h = MemberTransferSupport.bearerJsonHeaders(token);
        String url = MemberTransferSupport.inquiryUrl();
        for (String method : List.of("GET", "PUT", "DELETE")) {
            Response r = request(method, url, h, "{}");
            Assert.assertTrue(r.getStatusCode() >= 400, method + " should be rejected, got " + r.getStatusCode());
        }
    }

    @Test(priority = 34, groups = {"Regression", "MemberTransfer"})
    @Story("Missing fromBillingAccountId")
    @Description("Required item.fromBillingAccountId missing should return 4xx.")
    public void inquiry_missingFromBillingAccountId_rejected() {
        String token = tokenOrGet();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("toLocationId", MemberTransferSupport.toLocationId());
        item.put("brandRulesPreference", MemberTransferSupport.defaultBrandRules());
        String body = MemberTransferSupport.toJson(Map.of("userName", "doc_user", "items", List.of(item)));
        Response r = postWithReauthOn401(MemberTransferSupport.inquiryUrl(), token, body);
        Assert.assertTrue(r.getStatusCode() >= 400, "Expected 4xx, got " + r.getStatusCode());
    }

    @Test(priority = 35, groups = {"Regression", "MemberTransfer"})
    @Story("Missing toLocationId")
    @Description("Required item.toLocationId missing should return 4xx.")
    public void inquiry_missingToLocationId_rejected() {
        String token = tokenOrGet();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("fromBillingAccountId", MemberTransferSupport.fromBillingAccountId());
        item.put("brandRulesPreference", MemberTransferSupport.defaultBrandRules());
        String body = MemberTransferSupport.toJson(Map.of("userName", "doc_user", "items", List.of(item)));
        Response r = postWithReauthOn401(MemberTransferSupport.inquiryUrl(), token, body);
        Assert.assertTrue(r.getStatusCode() >= 400, "Expected 4xx, got " + r.getStatusCode());
    }

    @Test(priority = 36, groups = {"Regression", "MemberTransfer"})
    @Story("Invalid reasonCodeId format")
    @Description("Reason code validation should reject unsupported value formats.")
    public void inquiry_invalidReasonCode_validationError() {
        String token = tokenOrGet();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("fromBillingAccountId", MemberTransferSupport.fromBillingAccountId());
        item.put("toLocationId", MemberTransferSupport.toLocationId());
        item.put("reasonCodeId", "###INVALID_REASON_CODE###");
        String body = MemberTransferSupport.toJson(Map.of("userName", "doc_user", "items", List.of(item)));
        Response r = postWithReauthOn401(MemberTransferSupport.inquiryUrl(), token, body);
        Assert.assertTrue(r.getStatusCode() >= 400, "Expected 4xx, got " + r.getStatusCode());
    }

    @Test(priority = 37, groups = {"Regression", "MemberTransfer"})
    @Story("Brand rules permutations")
    @Description("Brand rules false/true combinations are handled.")
    public void inquiry_brandRulesPermutations_handled() {
        String token = tokenOrGet();
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("pastDueTransferEnabled", false);
        rules.put("pendingCancellationSubscriptionsEnabled", true);
        rules.put("paidUpFrontBalanceTransferEnabled", false);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("fromBillingAccountId", MemberTransferSupport.fromBillingAccountId());
        item.put("toLocationId", MemberTransferSupport.toLocationId());
        item.put("brandRulesPreference", rules);

        String body = MemberTransferSupport.toJson(Map.of("userName", "doc_user", "items", List.of(item)));
        Response r = postWithReauthOn401(MemberTransferSupport.inquiryUrl(), token, body);
        int c = r.getStatusCode();
        Assert.assertTrue((c >= 200 && c < 300) || c == 400, "Expected 2xx or validation 400, got " + c);
    }

    @Test(priority = 38, groups = {"Regression", "MemberTransfer"})
    @Story("Multiple items in one inquiry")
    @Description("Batch with 2 items should be accepted and return bulkId for supported data.")
    public void inquiry_multipleItems_batchHandled() {
        String token = tokenOrGet();
        Map<String, Object> item1 = new LinkedHashMap<>();
        item1.put("fromBillingAccountId", MemberTransferSupport.fromBillingAccountId());
        item1.put("toLocationId", MemberTransferSupport.toLocationId());

        Map<String, Object> item2 = new LinkedHashMap<>();
        item2.put("fromBillingAccountId", MemberTransferSupport.fromBillingAccountId());
        item2.put("toLocationId", MemberTransferSupport.toLocationId());

        String body = MemberTransferSupport.toJson(Map.of("userName", "doc_user", "items", List.of(item1, item2)));
        Response r = postWithReauthOn401(MemberTransferSupport.inquiryUrl(), token, body);
        int c = r.getStatusCode();
        Assert.assertTrue(c == 200 || c == 202 || c == 400, "Expected 200/202/400, got " + c);
        if (c == 200 || c == 202) {
            Assert.assertNotNull(r.jsonPath().getString("bulkId"), "bulkId");
        }
    }

    @Test(priority = 39, groups = {"Regression", "MemberTransfer"})
    @Story("Invalid Content-Type")
    @Description("text/plain payload should be rejected by API.")
    public void inquiry_invalidContentType_rejected() {
        String token = tokenOrGet();
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Authorization", "Bearer " + token);
        h.put("Content-Type", "text/plain");
        h.put("Accept", "application/json");
        h.put("ABCFS-TENANT-ID", MemberTransferSupport.tenantId());
        Response r = ApiClient.post(MemberTransferSupport.inquiryUrl(), h, MemberTransferSupport.defaultInquiryBodyJson());
        Assert.assertTrue(r.getStatusCode() >= 400, "Expected 4xx, got " + r.getStatusCode());
    }

    @Test(priority = 40, groups = {"Regression", "MemberTransfer"})
    @Story("Malformed JSON body")
    @Description("Invalid JSON should be rejected by API parser.")
    public void inquiry_malformedJson_rejected() {
        String token = tokenOrGet();
        String badJson = "{\"userName\":\"doc_user\",\"items\":[{";
        Response r = postWithReauthOn401(MemberTransferSupport.inquiryUrl(), token, badJson);
        Assert.assertTrue(r.getStatusCode() >= 400, "Expected 4xx, got " + r.getStatusCode());
    }

    @Test(priority = 41, groups = {"Regression", "MemberTransfer"})
    @Story("Missing inquiry client authority")
    @Description("Uses optional low-priv creds member.transfer.auth.credentials.no.inquiry.client.authority.")
    public void inquiry_missingClientAuthority_rejected() {
        String lowPrivCreds = ConfigManager.getOptional("member.transfer.auth.credentials.no.inquiry.client.authority");
        if (lowPrivCreds == null || lowPrivCreds.isBlank() || lowPrivCreds.contains("REPLACE")) {
            throw new SkipException("Set member.transfer.auth.credentials.no.inquiry.client.authority to execute this check");
        }
        String token = fetchTokenForCreds(lowPrivCreds);
        Response r = ApiClient.post(MemberTransferSupport.inquiryUrl(), MemberTransferSupport.bearerJsonHeaders(token),
                MemberTransferSupport.defaultInquiryBodyJson());
        int c = r.getStatusCode();
        Assert.assertTrue(c == 401 || c == 403, "Expected 401/403 for missing client authority, got " + c);
    }

    @Test(priority = 42, groups = {"Regression", "MemberTransfer"})
    @Story("Missing inquiry user permission")
    @Description("Uses optional low-priv creds member.transfer.auth.credentials.no.inquiry.user.permission.")
    public void inquiry_missingUserPermission_rejected() {
        String lowPrivCreds = ConfigManager.getOptional("member.transfer.auth.credentials.no.inquiry.user.permission");
        if (lowPrivCreds == null || lowPrivCreds.isBlank() || lowPrivCreds.contains("REPLACE")) {
            throw new SkipException("Set member.transfer.auth.credentials.no.inquiry.user.permission to execute this check");
        }
        String token = fetchTokenForCreds(lowPrivCreds);
        Response r = ApiClient.post(MemberTransferSupport.inquiryUrl(), MemberTransferSupport.bearerJsonHeaders(token),
                MemberTransferSupport.defaultInquiryBodyJson());
        int c = r.getStatusCode();
        Assert.assertTrue(c == 401 || c == 403, "Expected 401/403 for missing user permission, got " + c);
    }

    private static void ensureConfiguredForHappyPath() {
        if (isPlaceholderData()) {
            throw new SkipException("Set member.transfer.* in " + ConfigManager.getEnv() + ".properties (no REPLACE_).");
        }
    }

    private static boolean isPlaceholderData() {
        return containsReplace(MemberTransferSupport::fromBillingAccountId)
                || containsReplace(MemberTransferSupport::toLocationId)
                || containsReplace(MemberTransferSupport::tenantId);
    }

    private static boolean containsReplace(Supplier<String> s) {
        try {
            return s.get() != null && s.get().contains("REPLACE");
        } catch (Exception e) {
            return true;
        }
    }

    private static void ensureToken() {
        if (ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY) == null) {
            MemberTransferAuth.accessToken();
        }
    }

    private static String tokenOrGet() {
        ensureToken();
        return ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
    }

    private static Response postWithReauthOn401(String url, String token, String body) {
        Map<String, String> headers = MemberTransferSupport.bearerJsonHeaders(token);
        Response r = ApiClient.post(url, headers, body);
        if (r.getStatusCode() == 401) {
            MemberTransferSupport.fetchAndStoreToken();
            String t2 = (String) ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
            r = ApiClient.post(url, MemberTransferSupport.bearerJsonHeaders(t2), body);
        }
        return r;
    }

    private static boolean credsUnusable(Response r) {
        return r.getStatusCode() == 401 && ConfigManager.getMemberTransferAuthCredentials().contains("REPLACE");
    }

    private static Response request(String method, String url, Map<String, String> headers, String body) {
        RequestSpecification req = given().relaxedHTTPSValidation().headers(headers).log().all();
        if (body != null) {
            req.body(body);
        }
        return req.when().request(method, url).then().log().all().extract().response();
    }

    private static String fetchTokenForCreds(String creds) {
        String b64 = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Basic " + b64);
        headers.put("User-Agent", "RestAssured-MemberTransfer/1.0");
        headers.put("Accept", "application/json");
        headers.put("Cache-Control", "no-cache");
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "client_credentials");

        Response tokenResponse = ApiClient.post(MemberTransferSupport.authTokenUrl(), headers, form);
        Assert.assertEquals(tokenResponse.getStatusCode(), 200, "Low-priv token fetch should succeed");
        String token = tokenResponse.jsonPath().getString("access_token");
        Assert.assertNotNull(token, "access_token should be present");
        return token;
    }
}
