package com.externalAPIs.tests.Charge;

import io.qameta.allure.*;
import com.externalAPIs.config.ApiClient;
import com.externalAPIs.config.ConfigManager;
import com.externalAPIs.store.ResponseStore;
import com.externalAPIs.tests.auth.AuthUtils;
import com.externalAPIs.utils.DataProviderUtils;
import com.google.gson.GsonBuilder;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

/**
 * 🧩 Test Suite: ACH Payment Charge API
 * Validates POST /api/abcpg/payments/charge — initiates ACH charge transactions.
 */
@Epic("Payment Gateway")
@Feature("ACH Payment Charge API")
@Severity(SeverityLevel.CRITICAL)
public class CreateAchPaymentChargeTests {

    @Story("As a client, I can process an ACH charge using a secure payment token session.")
    @Test(priority = 1, dataProvider = "createAchPaymentChargeData", dataProviderClass = DataProviderUtils.class, groups = {"Regression"})
    @Description("Ensures ACH payment charges can be successfully created, validated, and stored for reporting.")
    public void createAchPaymentCharge(Map<String, Object> chargeData) {

        // Step 1️⃣ Token setup
        Allure.step("Fetching Secure Glofox Client Token for ACH charge request");
        String token = AuthUtils.getGlofoxSecureClientToken();
        Assert.assertNotNull(token, "❌ Missing Glofox Secure Client Token!");

        // Step 2️⃣ Endpoint
        String url = ConfigManager.get("secure.base.url") + "/api/abcpg/payments/charge";
        Allure.step("🔗 Endpoint: " + url);

        // Step 3️⃣ Headers (✅ FIXED TYPE ISSUE)
        String tenantId = java.util.Optional.ofNullable(ResponseStore.get("OrganizationId"))
                .map(Object::toString)
                .filter(s -> !s.isBlank() && !"null".equalsIgnoreCase(s))
                .orElse("41424320-4445-4620-4341-544547202020");

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020");
        headers.put("User-Agent", "Automation-Test");

        // Step 4️⃣ Request Body
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requests", new Object[]{chargeData});

        String requestBody = new GsonBuilder().setPrettyPrinting().create().toJson(payload);
        Allure.addAttachment("📦 ACH Payment Charge Request", "application/json", requestBody);

        Allure.step("🚀 Sending POST request to initiate ACH charge...");

        // Step 5️⃣ Execute
        Response response = ApiClient.post(url, headers, requestBody);
        ResponseStore.put("CreateAchPaymentChargeResponse", response.asString());

        // Step 6️⃣ Retry on 401
        if (response.statusCode() == 401) {
            Allure.step("⚠️ Unauthorized (401). Retrying with refreshed token...");
            token = AuthUtils.getGlofoxSecureClientToken();
            headers.put("Authorization", "Bearer " + token);
            response = ApiClient.post(url, headers, requestBody);
        }

        // Step 7️⃣ Log response
        int status = response.statusCode();
        String responseBody = response.asPrettyString();
        Allure.addAttachment("📨 ACH Payment Charge Response", "application/json", responseBody);
        Allure.step("📥 HTTP Response Code: " + status);

        // Step 8️⃣ Handle status codes
        switch (status) {
            case 200, 201, 204 -> {
                Allure.step("✅ ACH Payment Charge created successfully (HTTP " + status + ")");
                JsonPath json = response.jsonPath();

                String txnId = json.getString("responses[0].transactionId");
                String refId = json.getString("responses[0].referenceId");
                String amount = json.getString("responses[0].amount");
                String uniqId = json.getString("responses[0].metadata.uniqueTransactionId");

                Assert.assertNotNull(txnId, "❌ Transaction ID should not be null!");
                Assert.assertNotNull(amount, "❌ Transaction amount missing!");

                ResponseStore.put("AchTransactionId", txnId);
                ResponseStore.put("AchReferenceId", refId);
                ResponseStore.put("AchTransactionAmount", amount);
                ResponseStore.put("AchTransactionUniqueId", uniqId);

                Allure.step("📊 Stored Transaction Details ➜ " +
                        "\nTransaction ID: " + txnId +
                        "\nReference ID: " + refId +
                        "\nAmount: " + amount +
                        "\nUnique Transaction ID: " + uniqId);
            }

            case 400 -> Assert.fail("❌ 400 - Bad Request: Invalid request payload.");
            case 401 -> Assert.fail("❌ 401 - Unauthorized: Token invalid or expired.");
            case 403 -> Assert.fail("🚫 403 - Forbidden: Permission denied.");
            case 404 -> Assert.fail("❌ 404 - Not Found: Token or consumer missing.");
            case 409 -> Assert.fail("⚠️ 409 - Conflict: Duplicate ACH transaction.");
            case 422 -> Assert.fail("⚠️ 422 - Validation Error: Invalid ACH details.");
            case 500 -> Assert.fail("💥 500 - Internal Server Error.");
            case 502 -> Assert.fail("💥 502 - Bad Gateway: Upstream issue.");
            case 503 -> Assert.fail("🕒 503 - Service Unavailable.");
            case 504 -> Assert.fail("⌛ 504 - Gateway Timeout.");
            default -> Assert.fail("⚠️ Unexpected HTTP Status: " + status);
        }

        Allure.step("🔚 Test Completed for Create ACH Payment Charge API (Final Status: " + status + ")");
    }
}
