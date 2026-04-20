package com.externalAPIs.tests.Charge;
import io.qameta.allure.*;
import com.externalAPIs.config.ApiClient;
import com.externalAPIs.config.ConfigManager;
import com.externalAPIs.store.ResponseStore;
import com.externalAPIs.tests.auth.AuthUtils;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🧩 Test Suite: Merchant Details API
 * Validates GET /merchantDetails/{merchantId}
 */
@Epic("Payment Gateway")
@Feature("Merchant Details API")
public class GetMerchantDetailsTests {

    /**
     * 🎯 Positive Test Case – Valid Merchant Details Fetch
     */
    @Story("As a client, I can fetch merchant details using a secure token.")
    @Severity(SeverityLevel.CRITICAL)
    @Test(priority = 1, groups = {"Regression", "Smoke"})
    @Description("Fetch merchant details for a valid merchant ID and validate the response.")
    public void getMerchantDetails_Positive() {

        // Step 1️⃣ Token setup
        Allure.step("Fetching Secure Client Token for Merchant Details API");
        String token = AuthUtils.getSecureClientToken();
        Assert.assertNotNull(token, "❌ Missing secure auth token!");

        // Step 2️⃣ Merchant ID (valid)
        String merchantId = "11f0c912-5150-a7d7-b89a-ab6cd86ebe45";
        Allure.step("🆔 Using Merchant ID: " + merchantId);

        // Step 3️⃣ Read BASE URL from config file
        String baseUrl = ConfigManager.get("merchant.base.url");
        Assert.assertNotNull(baseUrl, "❌ merchant.base.url missing in config!");

        String url = baseUrl + "/merchantDetails/" + merchantId;
        Allure.step("🔗 Final Endpoint: " + url);

        // Step 4️⃣ Headers
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020");
        headers.put("User-Agent", "Automation-Test");

        Allure.step("🚀 Sending GET request for Merchant Details...");

        // Step 5️⃣ Execute
        Response response = ApiClient.get(url, headers);

        // Step 6️⃣ Retry on 401
        if (response.statusCode() == 401) {
            Allure.step("⚠️ Received 401. Retrying with refreshed token...");
            token = AuthUtils.getGlofoxSecureClientToken();
            headers.put("Authorization", "Bearer " + token);
            response = ApiClient.get(url, headers);
        }

        // Step 7️⃣ Capture response log
        String respBody = response.asPrettyString();
        Allure.addAttachment("📨 Merchant Details Response", "application/json", respBody);

        int status = response.statusCode();
        Allure.step("📥 HTTP Status Code: " + status);

        // Step 8️⃣ Validate success responses
        if (status == 200 || status == 201) {
            JsonPath json = response.jsonPath();

            String merchantName = json.getString("merchantName");
            String merchantStatus = json.getString("status");

            Assert.assertNotNull(merchantName, "❌ Merchant Name should not be null!");
            Assert.assertNotNull(merchantStatus, "❌ Merchant Status missing!");

            ResponseStore.put("MerchantDetailsResponse", respBody);

            Allure.step("📊 Stored Merchant Details:" +
                    "\nName: " + merchantName +
                    "\nStatus: " + merchantStatus);

        } else {
            Assert.fail("❌ Unexpected HTTP Status: " + status);
        }

        Allure.step("🔚 Test Completed: GET Merchant Details API");
    }


    // -----------------------------------------------------------
    // ❌ NEGATIVE TEST CASES
    // -----------------------------------------------------------

    @Test(priority = 2, groups = {"Regression"})
    @Description("Verify API returns 404/400 when using invalid merchant ID.")
    public void getMerchantDetails_InvalidMerchantId() {

        String invalidId = "invalid-merchant-123";
        String token = AuthUtils.getGlofoxSecureClientToken();

        String baseUrl = ConfigManager.get("merchant.base.url");
        String url = baseUrl + "/merchantDetails/" + invalidId;

        Allure.step("Testing with invalid Merchant ID: " + invalidId);
        Allure.step("URL: " + url);

        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + token,
                "ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020"
        );

        Response response = ApiClient.get(url, headers);
        Allure.addAttachment("Response", response.asPrettyString());

        int status = response.statusCode();
        Assert.assertTrue(status == 400 || status == 404,
                "❌ Expected 400/404 but got: " + status);
    }


    @Test(priority = 3)
    @Description("Verify API returns 401 when Authorization header is missing.")
    public void getMerchantDetails_MissingAuth() {

        String merchantId = "11f0c912-5150-a7d7-b89a-ab6cd86ebe45";

        String baseUrl = ConfigManager.get("merchant.base.url");
        String url = baseUrl + "/merchantDetails/" + merchantId;

        Map<String, String> headers = Map.of(
                "ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020"
        );

        Response response = ApiClient.get(url, headers);

        Assert.assertEquals(response.statusCode(), 401,
                "❌ Expected 401 Unauthorized");
    }


    @Test(priority = 4)
    @Description("Verify API returns 400/403 when Tenant ID is missing.")
    public void getMerchantDetails_MissingTenantId() {

        String merchantId = "11f0c912-5150-a7d7-b89a-ab6cd86ebe45";
        String token = AuthUtils.getGlofoxSecureClientToken();

        String baseUrl = ConfigManager.get("merchant.base.url");
        String url = baseUrl + "/merchantDetails/" + merchantId;

        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + token
        );

        Response response = ApiClient.get(url, headers);
        int status = response.statusCode();

        Assert.assertTrue(status == 400 || status == 403,
                "❌ Expected 400/403 but got: " + status);
    }


    @Test(priority = 5)
    @Description("Verify API returns 401 for invalid/expired token.")
    public void getMerchantDetails_InvalidToken() {

        String merchantId = "11f0c912-5150-a7d7-b89a-ab6cd86ebe45";

        String baseUrl = ConfigManager.get("merchant.base.url");
        String url = baseUrl + "/merchantDetails/" + merchantId;

        Map<String, String> headers = Map.of(
                "Authorization", "Bearer invalid-token",
                "ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020"
        );

        Response response = ApiClient.get(url, headers);

        Assert.assertEquals(response.statusCode(), 401,
                "❌ Expected 401 Unauthorized");
    }


    @Test(priority = 6)
    @Description("Verify API returns 403 when Tenant ID is mismatched.")
    public void getMerchantDetails_TenantMismatch() {

        String merchantId = "11f0c912-5150-a7d7-b89a-ab6cd86ebe45";
        String token = AuthUtils.getGlofoxSecureClientToken();

        String baseUrl = ConfigManager.get("merchant.base.url");
        String url = baseUrl + "/merchantDetails/" + merchantId;

        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + token,
                "ABCFS-TENANT-ID", "00000000-0000-0000-0000-000000000000"
        );

        Response response = ApiClient.get(url, headers);

        Assert.assertEquals(response.statusCode(), 403,
                "❌ Expected 403 Forbidden (Tenant Mismatch)");
    }
}
