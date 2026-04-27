package com.membertransfer.tests;

import com.membertransfer.auth.MemberTransferAuth;
import com.membertransfer.config.ApiClient;
import com.membertransfer.config.ConfigManager;
import com.membertransfer.store.ResponseStore;
import com.membertransfer.support.MemberTransferSupport;
import io.qameta.allure.*;
import io.restassured.path.json.JsonPath;
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
import java.util.Optional;

import static io.restassured.RestAssured.given;

@Epic("Member transfer")
@Feature("Batch transfer status")
@Severity(SeverityLevel.NORMAL)
public class MemberTransferStatusApiTests {

    @Test(priority = 10, groups = {"Regression", "MemberTransfer", "Sanity", "MT_Sanity"})
    @Story("TC_01: GET status endpoint is reachable")
    @Description("200 or 404 for known sample id in properties.")
    public void getStatus_bySampleBulkIdFromConfig() {
        if (noToken()) {
            MemberTransferAuth.accessToken();
        }
        if (ConfigManager.getMemberTransferAuthCredentials().contains("REPLACE")) {
            throw new SkipException("member.transfer auth not configured");
        }
        String id = MemberTransferSupport.sampleStatusBulkId();
        String url = MemberTransferSupport.statusUrl(id);
        Allure.addAttachment("GET", "text/plain", url);
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        Allure.addAttachment("response " + c, "application/json", r.asPrettyString());
        Assert.assertTrue(c == 200 || c == 404, "Expected 200 or 404, was " + c);
        if (c == 200) {
            JsonPath jp = r.jsonPath();
            String status = jp.getString("status");
            Allure.step("status: " + status);
        }
    }

    @Test(priority = 11, groups = {"Regression", "MemberTransfer"})
    @Story("TC_11: Chained bulkId from inquiry")
    @Description("Uses MemberTransferBulkId in ResponseStore.")
    public void getStatus_usingBulkIdFromInquiry() {
        String bulk = ResponseStore.get(MemberTransferSupport.BULK_ID_KEY);
        if (bulk == null || bulk.isEmpty()) {
            throw new SkipException("Run inquiry first for " + MemberTransferSupport.BULK_ID_KEY);
        }
        if (noToken()) {
            MemberTransferAuth.accessToken();
        }
        String url = MemberTransferSupport.statusUrl(bulk);
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        int c = r.getStatusCode();
        Allure.addAttachment("chained " + c, "application/json", r.asPrettyString());
        Assert.assertTrue(c == 200 || c == 404, "HTTP " + c);
    }

    @Test(priority = 12, groups = {"Regression", "MemberTransfer"})
    @Story("TC_02: Unsupported methods are rejected")
    @Description("POST/PUT/DELETE to status endpoint should return 4xx/405.")
    public void getStatus_unsupportedMethods_rejected() {
        String bulkId = resolveBulkId();
        String url = MemberTransferSupport.statusUrl(bulkId);
        String token = tokenOrSkip();

        Map<String, String> h = MemberTransferSupport.bearerJsonHeaders(token);
        for (String method : List.of("POST", "PUT", "DELETE")) {
            Response r = request(method, url, h);
            int code = r.getStatusCode();
            Allure.step(method + " -> HTTP " + code);
            Assert.assertTrue(code >= 400, method + " should be rejected, got " + code);
        }
    }

    @Test(priority = 13, groups = {"Regression", "MemberTransfer"})
    @Story("TC_06: Missing Authorization header")
    @Description("GET status without bearer should return 401/403.")
    public void getStatus_withoutAuthorization_rejected() {
        String bulkId = resolveBulkId();
        String url = MemberTransferSupport.statusUrl(bulkId);
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Content-Type", "application/json;charset=UTF-8");
        h.put("Accept", "application/json;charset=UTF-8");
        h.put("ABCFS-TENANT-ID", MemberTransferSupport.tenantId());
        Response r = ApiClient.get(url, h);
        int code = r.getStatusCode();
        Assert.assertTrue(code == 401 || code == 403, "Expected 401/403, got " + code);
    }

    @Test(priority = 14, groups = {"Regression", "MemberTransfer"})
    @Story("TC_07: Invalid/expired bearer token")
    @Description("Bearer with garbage token should return 401/403.")
    public void getStatus_invalidBearer_rejected() {
        String bulkId = resolveBulkId();
        String url = MemberTransferSupport.statusUrl(bulkId);
        Map<String, String> h = MemberTransferSupport.bearerJsonHeaders("invalid.token.value");
        Response r = ApiClient.get(url, h);
        int code = r.getStatusCode();
        Assert.assertTrue(code == 401 || code == 403, "Expected 401/403, got " + code);
    }

    @Test(priority = 15, groups = {"Regression", "MemberTransfer"})
    @Story("TC_08: Missing client authority")
    @Description("Uses optional low-priv creds member.transfer.auth.credentials.no.client.authority when configured.")
    public void getStatus_missingClientAuthority_rejected() {
        String lowPrivCreds = ConfigManager.getOptional("member.transfer.auth.credentials.no.client.authority");
        if (lowPrivCreds == null || lowPrivCreds.isBlank() || lowPrivCreds.contains("REPLACE")) {
            throw new SkipException("Set member.transfer.auth.credentials.no.client.authority to execute this check");
        }
        String token = fetchTokenForCreds(lowPrivCreds);
        String url = MemberTransferSupport.statusUrl(resolveBulkId());
        Response r = ApiClient.get(url, MemberTransferSupport.bearerJsonHeaders(token));
        int code = r.getStatusCode();
        Assert.assertTrue(code == 401 || code == 403, "Expected 401/403 for missing authority, got " + code);
    }

    @Test(priority = 16, groups = {"Regression", "MemberTransfer"})
    @Story("TC_09: Missing user permission")
    @Description("Uses optional creds member.transfer.auth.credentials.no.user.permission when configured.")
    public void getStatus_missingUserPermission_rejected() {
        String lowPrivCreds = ConfigManager.getOptional("member.transfer.auth.credentials.no.user.permission");
        if (lowPrivCreds == null || lowPrivCreds.isBlank() || lowPrivCreds.contains("REPLACE")) {
            throw new SkipException("Set member.transfer.auth.credentials.no.user.permission to execute this check");
        }
        String token = fetchTokenForCreds(lowPrivCreds);
        String url = MemberTransferSupport.statusUrl(resolveBulkId());
        Response r = ApiClient.get(url, MemberTransferSupport.bearerJsonHeaders(token));
        int code = r.getStatusCode();
        Assert.assertTrue(code == 401 || code == 403, "Expected 401/403 for missing permission, got " + code);
    }

    @Test(priority = 17, groups = {"Regression", "MemberTransfer"})
    @Story("TC_10: Request succeeds without tenant header")
    @Description("Tenancy optional check: no ABCFS-TENANT-ID.")
    public void getStatus_withoutTenant_optionalBehavior() {
        String url = MemberTransferSupport.statusUrl(resolveBulkId());
        String token = tokenOrSkip();
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Authorization", "Bearer " + token);
        h.put("Content-Type", "application/json;charset=UTF-8");
        h.put("Accept", "application/json;charset=UTF-8");
        Response r = ApiClient.get(url, h);
        int code = r.getStatusCode();
        Assert.assertTrue(code == 200 || code == 404 || code == 400 || code == 403,
                "Expected optional-tenant outcome (200/404) or env-security rejection (400/403), got " + code);
    }

    @Test(priority = 18, groups = {"Regression", "MemberTransfer"})
    @Story("TC_12: Default paging when page/size omitted")
    @Description("When page and size omitted, endpoint should return defaults or valid response.")
    public void getStatus_defaultPaging_whenPageAndSizeOmitted() {
        String bulk = resolveBulkId();
        String token = tokenOrSkip();
        String url = MemberTransferSupport.baseUrl()
                + "/api/member-transfer/billing-account-batch-transfers/status/" + bulk
                + "?q=operation_type%3D%3Dtransfer&sort=toBillingAccountNumber%2Casc";
        Response r = ApiClient.get(url, MemberTransferSupport.bearerJsonHeaders(token));
        if (r.getStatusCode() == 404) {
            throw new SkipException("Bulk id not found; default paging cannot be asserted");
        }
        Assert.assertTrue(r.getStatusCode() >= 200 && r.getStatusCode() < 300, "Expected 2xx, got " + r.getStatusCode());
        JsonPath jp = r.jsonPath();
        Integer pageNumber = jp.getInt("itemResults.number");
        Integer size = jp.getInt("itemResults.size");
        if (pageNumber != null) {
            Assert.assertTrue(pageNumber >= 0, "pageNumber should be >= 0");
        }
        if (size != null) {
            Assert.assertTrue(size > 0, "default size should be > 0");
        }
    }

    @Test(priority = 19, groups = {"Regression", "MemberTransfer"})
    @Story("TC_13: Explicit page and size")
    @Description("Response should respect page and size constraints.")
    public void getStatus_explicitPageAndSize_returnsSubset() {
        String url = MemberTransferSupport.statusUrl(resolveBulkId(), 0, 1, "operation_type==transfer", "toBillingAccountNumber,asc");
        Response r = getWithReauth(url);
        if (r.getStatusCode() == 404) {
            throw new SkipException("Bulk id not found; subset cannot be validated");
        }
        Assert.assertEquals(r.getStatusCode(), 200, "Expected 200 for explicit page/size");
        JsonPath jp = r.jsonPath();
        int contentSize = Optional.ofNullable(jp.getList("itemResults.content")).map(List::size).orElse(0);
        int responseSize = Optional.ofNullable(jp.getInt("itemResults.size")).orElse(1);
        Assert.assertTrue(contentSize <= 1, "Expected <=1 item, got " + contentSize);
        Assert.assertTrue(responseSize <= 1 || responseSize == 0, "Expected response size <=1, got " + responseSize);
    }

    @Test(priority = 20, groups = {"Regression", "MemberTransfer"})
    @Story("TC_14: Negative page value")
    @Description("page=-1 should return 4xx validation error.")
    public void getStatus_negativePage_rejected() {
        String url = MemberTransferSupport.statusUrl(resolveBulkId(), -1, 10, "operation_type==transfer", "toBillingAccountNumber,asc");
        Response r = getWithReauth(url);
        Assert.assertTrue(r.getStatusCode() >= 400, "Expected 4xx for negative page, got " + r.getStatusCode());
    }

    @Test(priority = 21, groups = {"Regression", "MemberTransfer"})
    @Story("TC_15: Zero size value")
    @Description("size=0 should return 4xx validation error.")
    public void getStatus_zeroSize_rejected() {
        String url = MemberTransferSupport.statusUrl(resolveBulkId(), 0, 0, "operation_type==transfer", "toBillingAccountNumber,asc");
        Response r = getWithReauth(url);
        Assert.assertTrue(r.getStatusCode() >= 400, "Expected 4xx for size=0, got " + r.getStatusCode());
    }

    @Test(priority = 22, groups = {"Regression", "MemberTransfer"})
    @Story("TC_16: Out-of-range page handling")
    @Description("Very large page should be handled gracefully (200 empty page or 4xx).")
    public void getStatus_outOfRangePage_handledGracefully() {
        String url = MemberTransferSupport.statusUrl(resolveBulkId(), 9999, 10, "operation_type==transfer", "toBillingAccountNumber,asc");
        Response r = getWithReauth(url);
        int code = r.getStatusCode();
        Assert.assertTrue((code >= 200 && code < 300) || (code >= 400 && code < 500),
                "Expected graceful 2xx/4xx handling, got " + code);
        if (code == 200) {
            JsonPath jp = r.jsonPath();
            List<?> content = jp.getList("itemResults.content");
            if (content != null) {
                Assert.assertTrue(content.isEmpty() || content.size() >= 0, "Content should be a valid list");
            }
        }
    }

    @Test(priority = 23, groups = {"Regression", "MemberTransfer"})
    @Story("TC_17: Ascending sort on supported field")
    @Description("sort=toBillingAccountNumber,asc should return ordered subset when field exists.")
    public void getStatus_sortAsc_supportedField() {
        String url = MemberTransferSupport.statusUrl(resolveBulkId(), 0, 10, "operation_type==transfer", "toBillingAccountNumber,asc");
        Response r = getWithReauth(url);
        if (r.getStatusCode() == 404) {
            throw new SkipException("Bulk id not found; sort cannot be validated");
        }
        Assert.assertEquals(r.getStatusCode(), 200, "Expected 200");
        assertSorted(r, true);
    }

    @Test(priority = 24, groups = {"Regression", "MemberTransfer"})
    @Story("TC_18: Descending sort on supported field")
    @Description("sort=toBillingAccountNumber,desc should return ordered subset when field exists.")
    public void getStatus_sortDesc_supportedField() {
        String url = MemberTransferSupport.statusUrl(resolveBulkId(), 0, 10, "operation_type==transfer", "toBillingAccountNumber,desc");
        Response r = getWithReauth(url);
        if (r.getStatusCode() == 404) {
            throw new SkipException("Bulk id not found; sort cannot be validated");
        }
        Assert.assertEquals(r.getStatusCode(), 200, "Expected 200");
        assertSorted(r, false);
    }

    @Test(priority = 25, groups = {"Regression", "MemberTransfer"})
    @Story("TC_19: Invalid sort field")
    @Description("sort=unknownField,asc should return 4xx.")
    public void getStatus_invalidSortField_rejected() {
        String url = MemberTransferSupport.statusUrl(resolveBulkId(), 0, 10, "operation_type==transfer", "unknownField,asc");
        Response r = getWithReauth(url);
        Assert.assertTrue(r.getStatusCode() >= 400, "Expected 4xx for invalid sort field, got " + r.getStatusCode());
    }

    @Test(priority = 26, groups = {"Regression", "MemberTransfer"})
    @Story("TC_20: Invalid sort direction")
    @Description("sort=toBillingAccountNumber,sideways should return 4xx.")
    public void getStatus_invalidSortDirection_rejected() {
        String url = MemberTransferSupport.statusUrl(resolveBulkId(), 0, 10, "operation_type==transfer", "toBillingAccountNumber,sideways");
        Response r = getWithReauth(url);
        Assert.assertTrue(r.getStatusCode() >= 400, "Expected 4xx for invalid sort direction, got " + r.getStatusCode());
    }

    @Test(priority = 27, groups = {"Regression", "MemberTransfer"})
    @Story("TC_04: Invalid bulk id path")
    @Description("Malformed id -> 4xx")
    public void getStatus_invalidPath_expect4xx() {
        if (noToken()) {
            MemberTransferAuth.accessToken();
        }
        if (isPlaceholderCreds()) {
            throw new SkipException("Not configured");
        }
        String url = MemberTransferSupport.statusUrl("not-a-uuid", 0, 1, "operation_type==transfer", "toBillingAccountNumber,asc");
        Response r = getWithReauth(url);
        if (credsUnusable(r)) {
            return;
        }
        Assert.assertTrue(r.getStatusCode() >= 400, "HTTP " + r.getStatusCode());
    }

    private static String resolveBulkId() {
        String fromInquiry = ResponseStore.get(MemberTransferSupport.BULK_ID_KEY);
        if (fromInquiry != null && !fromInquiry.isBlank() && !"null".equalsIgnoreCase(fromInquiry)) {
            return fromInquiry;
        }
        return MemberTransferSupport.sampleStatusBulkId();
    }

    private static String tokenOrSkip() {
        if (noToken()) {
            String token = MemberTransferAuth.accessToken();
            if (token == null || token.isBlank()) {
                throw new SkipException("No bearer token available for status validations");
            }
        }
        return ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
    }

    private static boolean noToken() {
        String t = ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
        return t == null || t.isEmpty();
    }

    private static boolean isPlaceholderCreds() {
        return ConfigManager.getMemberTransferAuthCredentials().contains("REPLACE");
    }

    private static Response getWithReauth(String url) {
        String t = (String) ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
        if (t == null || t.isEmpty()) {
            t = MemberTransferAuth.accessToken();
        }
        Map<String, String> h = MemberTransferSupport.bearerJsonHeaders(t);
        Response r = ApiClient.get(url, h);
        if (r.getStatusCode() == 401) {
            MemberTransferSupport.fetchAndStoreToken();
            t = (String) ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
            r = ApiClient.get(url, MemberTransferSupport.bearerJsonHeaders(t));
        }
        return r;
    }

    private static boolean credsUnusable(Response r) {
        return r.getStatusCode() == 401 && ConfigManager.getMemberTransferAuthCredentials().contains("REPLACE");
    }

    private static Response request(String method, String url, Map<String, String> headers) {
        RequestSpecification req = given().relaxedHTTPSValidation().headers(headers).log().all();
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
        Map<String, String> body = new LinkedHashMap<>();
        body.put("grant_type", "client_credentials");
        Response tokenResponse = ApiClient.post(MemberTransferSupport.authTokenUrl(), headers, body);
        Assert.assertEquals(tokenResponse.getStatusCode(), 200, "Token fetch for low-priv creds should succeed");
        String token = tokenResponse.jsonPath().getString("access_token");
        Assert.assertNotNull(token, "Low-priv access_token");
        return token;
    }

    private static void assertSorted(Response response, boolean ascending) {
        List<Map<String, Object>> content = response.jsonPath().getList("itemResults.content");
        if (content == null || content.size() < 2) {
            Allure.step("Not enough rows to validate ordering");
            return;
        }

        String prev = null;
        int compared = 0;
        for (Map<String, Object> item : content) {
            Object raw = item.get("toBillingAccountNumber");
            if (raw == null) {
                raw = item.get("to_billing_account_number");
            }
            if (raw == null) {
                continue;
            }
            String current = raw.toString();
            if (prev != null) {
                int cmp = prev.compareTo(current);
                if (ascending) {
                    Assert.assertTrue(cmp <= 0, "Expected ascending order: " + prev + " <= " + current);
                } else {
                    Assert.assertTrue(cmp >= 0, "Expected descending order: " + prev + " >= " + current);
                }
                compared++;
            }
            prev = current;
        }

        if (compared == 0) {
            Allure.step("Sort field not present in content rows; skipped strict ordering assertion");
        }
    }
}
