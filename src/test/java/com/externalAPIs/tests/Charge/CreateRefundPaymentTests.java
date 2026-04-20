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
 * 🧩 Test Suite: Payment Refund API
 * Validates POST /api/payment-gateway/payments/refund — processes refund requests for previous ACH charge transactions.
 */
@Epic("Payment Gateway")
@Feature("Refund Payment API")
@Severity(SeverityLevel.CRITICAL)
public class CreateRefundPaymentTests {

    @Story("As a client, I can initiate a payment refund using a valid ACH charge referenceId and token.")
    @Test(priority = 2, dataProvider = "createRefundPaymentData", dataProviderClass = DataProviderUtils.class, groups = {"Regression"})
    @Description("Ensures refunds are processed successfully by linking them to previous ACH charge transactions.")
    public void createRefundPayment(Map<String, Object> refundData) {

        // Step 1️⃣ Fetch Auth Token
        Allure.step("Fetching Secure Glofox Client Token for Refund request");
        String token = AuthUtils.getGlofoxSecureClientToken();
        Assert.assertNotNull(token, "❌ Missing Glofox Secure Client Token!");

        // Step 2️⃣ Endpoint setup
        String url = "https://api-dev.pgw.abc.fitness" + "/api/payment-gateway/payments/refund";
        Allure.step("🔗 Endpoint: " + url);

        // Step 3️⃣ Headers
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020");
        headers.put("User-Agent", "Automation-Test");

        // Step 4️⃣ Dynamic data injection
        Object storedToken = ResponseStore.get("AchTransactionTokenId");
        if (storedToken != null && !storedToken.toString().trim().isEmpty()) {
            refundData.put("tokenId", storedToken.toString());
            Allure.step("🔄 Using dynamic tokenId from previous charge: " + storedToken);
        }

        Object storedTerminal = ResponseStore.get("TerminalId");
        if (storedTerminal != null && !storedTerminal.toString().trim().isEmpty()) {
            refundData.put("terminalId", storedTerminal.toString());
            Allure.step("🔄 Using dynamic terminalId from previous flow: " + storedTerminal);
        }

        // ✅ Correctly link refund to the previous charge via referenceId
        Object storedRefId = ResponseStore.get("AchReferenceId");
        if (storedRefId != null && !storedRefId.toString().trim().isEmpty()) {
            refundData.put("referenceId", storedRefId.toString());
            Allure.step("🔗 Linked refund with previous charge referenceId: " + storedRefId);
        } else {
            refundData.put("referenceId", UUID.randomUUID().toString());
            Allure.step("⚠️ No previous charge referenceId found. Generated new referenceId.");
        }

        // Step 5️⃣ Build request payload
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requests", new Object[]{refundData});

        String requestBody = new GsonBuilder().setPrettyPrinting().create().toJson(payload);
        Allure.addAttachment("📦 Refund Payment Request", "application/json", requestBody);
        Allure.step("🚀 Sending POST request to initiate refund...");

        // Step 6️⃣ Execute request
        Response response = ApiClient.post(url, headers, requestBody);
        ResponseStore.put("CreateRefundPaymentResponse", response.asString());

        // Step 7️⃣ Retry logic if unauthorized
        if (response.statusCode() == 401) {
            Allure.step("⚠️ Unauthorized (401). Retrying with refreshed token...");
            token = AuthUtils.getGlofoxSecureClientToken();
            headers.put("Authorization", "Bearer " + token);
            response = ApiClient.post(url, headers, requestBody);
        }

        // Step 8️⃣ Parse and validate response
        int status = response.statusCode();
        String responseBody = response.asPrettyString();
        Allure.addAttachment("📨 Refund Payment Response", "application/json", responseBody);
        Allure.step("📥 HTTP Response Code: " + status);

        switch (status) {
            case 200, 201, 204 -> {
                Allure.step("✅ Refund created successfully (HTTP " + status + ")");
                JsonPath json = response.jsonPath();

                String refundTxnId = json.getString("responses[0].transactionId");
                String refundRefId = json.getString("responses[0].referenceId");
                String refundAmt = json.getString("responses[0].amount");

                Assert.assertNotNull(refundTxnId, "❌ Refund Transaction ID should not be null!");
                Assert.assertNotNull(refundAmt, "❌ Refund amount missing!");

                ResponseStore.put("RefundTransactionId", refundTxnId);
                ResponseStore.put("RefundReferenceId", refundRefId);
                ResponseStore.put("RefundAmount", refundAmt);

                Allure.step("📊 Stored Refund Details ➜ " +
                        "\nTransaction ID: " + refundTxnId +
                        "\nReference ID: " + refundRefId +
                        "\nAmount: " + refundAmt);
            }

            case 400 -> Assert.fail("❌ 400 - Bad Request: Invalid refund payload.");
            case 401 -> Assert.fail("❌ 401 - Unauthorized: Token invalid or expired.");
            case 403 -> Assert.fail("🚫 403 - Forbidden: Access denied.");
            case 404 -> Assert.fail("❌ 404 - Not Found: Charge transaction not found for refund.");
            case 409 -> Assert.fail("⚠️ 409 - Conflict: Duplicate refund request.");
            case 422 -> Assert.fail("⚠️ 422 - Validation Error: Refund details invalid.");
            case 500 -> Assert.fail("💥 500 - Internal Server Error.");
            default -> Assert.fail("⚠️ Unexpected HTTP Status: " + status);
        }

        Allure.step("🔚 Test Completed for Refund Payment API (Final Status: " + status + ")");
    }
}
