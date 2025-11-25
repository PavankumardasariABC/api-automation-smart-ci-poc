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
 * 🧩 Test Suite: Transaction Details API
 * Validates GET /transactionDetails/{transactionId}
 */
@Epic("Payment Gateway")
@Feature("Transaction Details API")
public class GetTransactionDetailsTests {

    /**
     * 🎯 Positive Test Case – Fetch transaction details using a valid ID.
     */
    @Story("As a client, I can fetch transaction details using a secure token.")
    @Severity(SeverityLevel.CRITICAL)
    @Test(priority = 1, groups = {"Regression", "Smoke"})
    @Description("Fetches transaction details for a valid transaction ID and validates response.")
    public void getTransactionDetails_Positive() {

        // Step 1️⃣ Token setup
        Allure.step("Fetching Secure Token for Transaction Details request");
        String token = AuthUtils.getSecureClientToken();
        Assert.assertNotNull(token, "❌ Missing secure auth token!");

        // Step 2️⃣ Transaction ID
        String txnId = "11f0c5eb-ffd0-1aa1-8a5a-c3b6d225d7c8";
        Allure.step("🆔 Using Transaction ID: " + txnId);

        // Step 3️⃣ Read BASE URL from config file
        String baseUrl = ConfigManager.get("transactions.base.url");
        Assert.assertNotNull(baseUrl, "❌ transactions.base.url missing in config!");

        String url = baseUrl + "/transactionDetails/" + txnId;
        Allure.step("🔗 Final Endpoint: " + url);

        // Step 4️⃣ Headers
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020");
        headers.put("User-Agent", "Automation-Test");

        Allure.step("🚀 Sending GET request to fetch transaction details...");

        // Step 5️⃣ Execute
        Response response = ApiClient.get(url, headers);

        // Step 6️⃣ Retry on 401
        if (response.statusCode() == 401) {
            Allure.step("⚠️ Unauthorized (401). Retrying with refreshed token...");
            token = AuthUtils.getGlofoxSecureClientToken();
            headers.put("Authorization", "Bearer " + token);
            response = ApiClient.get(url, headers);
        }

        // Step 7️⃣ Log response
        String responseBody = response.asPrettyString();
        Allure.addAttachment("📨 Transaction Details Response", "application/json", responseBody);

        int status = response.statusCode();
        Allure.step("📥 HTTP Response Code: " + status);

        // Step 8️⃣ Status Handling
        if (status == 200 || status == 201) {
            JsonPath json = response.jsonPath();

            String amount = json.getString("amount");
            String transactionType = json.getString("transactionType");

            Assert.assertNotNull(amount, "❌ Transaction Amount should not be null!");
            Assert.assertNotNull(transactionType, "❌ Transaction Type missing!");

            ResponseStore.put("TransactionDetailsResponse", responseBody);

            Allure.step("📊 Stored Transaction Details: Amount: " + amount +
                    ", Type: " + transactionType);

        } else {
            Assert.fail("❌ Unexpected HTTP Status: " + status);
        }

        Allure.step("🔚 Test Completed for GET Transaction Details API");
    }

    // -----------------------------------------------------------
    // ❌ NEGATIVE TEST CASES
    // -----------------------------------------------------------

    @Test(priority = 2, groups = {"Regression"})
    @Description("Verify API returns 404/400 for invalid transaction ID.")
    public void getTransactionDetails_InvalidId() {

        String invalidTxnId = "invalid-123";
        String token = AuthUtils.getGlofoxSecureClientToken();

        String baseUrl = ConfigManager.get("transactions.base.url");
        String url = baseUrl + "/transactionDetails/" + invalidTxnId;

        Allure.step("Testing with invalid transaction ID: " + invalidTxnId);
        Allure.step("URL: " + url);

        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + token,
                "ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020"
        );

        Response response = ApiClient.get(url, headers);
        int status = response.statusCode();
        Allure.addAttachment("Response", response.asPrettyString());

        Assert.assertTrue(status == 400 || status == 404,
                "Expected 400/404, but got: " + status);
    }


    @Test(priority = 3)
    @Description("Verify API returns 401 when Authorization header is missing.")
    public void getTransactionDetails_MissingAuth() {

        String txnId = "11f0c5eb-ffd0-1aa1-8a5a-c3b6d225d7c8";
        String baseUrl = ConfigManager.get("transactions.base.url");

        String url = baseUrl + "/transactionDetails/" + txnId;

        Map<String, String> headers = Map.of(
                "ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020"
        );

        Response response = ApiClient.get(url, headers);

        Assert.assertEquals(response.statusCode(), 401,
                "❌ Expected 401 Unauthorized");
    }


    @Test(priority = 4)
    @Description("Verify API returns 400/403 when tenant ID header is missing.")
    public void getTransactionDetails_MissingTenantId() {

        String txnId = "11f0c5eb-ffd0-1aa1-8a5a-c3b6d225d7c8";
        String token = AuthUtils.getGlofoxSecureClientToken();

        String baseUrl = ConfigManager.get("transactions.base.url");
        String url = baseUrl + "/transactionDetails/" + txnId;

        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + token
        );

        Response response = ApiClient.get(url, headers);

        int status = response.statusCode();

        Assert.assertTrue(status == 400 || status == 403,
                "❌ Expected 400/403, but got: " + status);
    }


    @Test(priority = 5)
    @Description("Verify API returns 401 for invalid or expired token.")
    public void getTransactionDetails_InvalidToken() {

        String txnId = "11f0c5eb-ffd0-1aa1-8a5a-c3b6d225d7c8";
        String baseUrl = ConfigManager.get("transactions.base.url");

        String url = baseUrl + "/transactionDetails/" + txnId;

        Map<String, String> headers = Map.of(
                "Authorization", "Bearer invalid-token",
                "ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020"
        );

        Response response = ApiClient.get(url, headers);

        Assert.assertEquals(response.statusCode(), 401,
                "❌ Expected 401 Unauthorized");
    }


    @Test(priority = 6)
    @Description("Verify API returns 403 when tenant mismatch occurs.")
    public void getTransactionDetails_Forbidden() {

        String txnId = "11f0c5eb-ffd0-1aa1-8a5a-c3b6d225d7c8";
        String token = AuthUtils.getGlofoxSecureClientToken();

        String baseUrl = ConfigManager.get("transactions.base.url");
        String url = baseUrl + "/transactionDetails/" + txnId;

        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + token,
                "ABCFS-TENANT-ID", "00000000-0000-0000-0000-000000000000"
        );

        Response response = ApiClient.get(url, headers);

        Assert.assertEquals(response.statusCode(), 403,
                "❌ Expected 403 Forbidden (Tenant Mismatch)");
    }
}
