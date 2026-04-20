package com.externalAPIs.tests.common;

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
 * 🧩 Test Suite: Bank Payment Token API
 * Validates POST /api/abcpg/payment-token/bank — registration of a new bank account.
 */
@Epic("Payment Gateway")
@Feature("Create Bank Payment Token API")
@Severity(SeverityLevel.CRITICAL)
public class CreateBankPaymentTokenTests {

    @Story("As a user, I can register a bank account and receive a token ID for secure storage.")
    @Test(dataProvider = "createBankPaymentTokenData", dataProviderClass = DataProviderUtils.class, groups = {"Regression"})
    @Description("Ensures that new bank payment tokens can be created successfully, returning a valid token ID for linking.")
    public void createBankPaymentToken(Map<String, Object> bankData) {

        // Step 1️⃣: Fetch Secure Client Token
        Allure.step("🔑 Fetch Secure Client Token for authorization");
        String secureToken = AuthUtils.getSecureClientToken();
        Assert.assertNotNull(secureToken, "❌ SecureClientToken should not be null!");

        // Step 2️⃣: Define endpoint
        String url = ConfigManager.get("secure.base.url") + "/api/abcpg/payment-token/bank";
        Allure.step("🔗 Endpoint: " + url);

        // Step 3️⃣: Prepare headers (✅ FIXED TYPE-SAFE TENANT ID)
        String tenantId = java.util.Optional.ofNullable(ResponseStore.get("OrganizationId"))
                .map(Object::toString)
                .filter(s -> !s.isBlank() && !"null".equalsIgnoreCase(s))
                .orElse("41424320-4445-4620-4341-544547202020");

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + secureToken);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("ABCFS-TENANT-ID", tenantId);
        headers.put("User-Agent", "Automation-Test");

        // Step 4️⃣: Prepare request payload
        String requestBody = new GsonBuilder().setPrettyPrinting().create().toJson(bankData);
        Allure.addAttachment("📦 Bank Payment Token Request", "application/json", requestBody);

        Allure.step("🚀 Sending POST request to create Bank Payment Token...");

        // Step 5️⃣: Execute API call
        Response response = ApiClient.post(url, headers, requestBody);
        ResponseStore.put("CreateBankPaymentTokenResponse", response.asString());

        // Step 6️⃣: Retry on Unauthorized
        if (response.statusCode() == 401) {
            Allure.step("⚠️ Unauthorized (401). Retrying with refreshed Secure Token...");
            secureToken = AuthUtils.getSecureClientToken();
            headers.put("Authorization", "Bearer " + secureToken);
            response = ApiClient.post(url, headers, requestBody);
        }

        // Step 7️⃣: Log response
        int status = response.statusCode();
        String responseBody = response.asPrettyString();
        Allure.addAttachment("📨 Bank Payment Token Response", "application/json", responseBody);
        Allure.step("📥 HTTP Response Code: " + status);

        // Step 8️⃣: Handle all possible responses
        switch (status) {
            // ✅ SUCCESS CASES
            case 200, 201, 204 -> {
                Allure.step("✅ Bank Payment Token created successfully (HTTP " + status + ")");
                JsonPath json = response.jsonPath();

                String bankTokenId = json.getString("id");
                String lastFour = json.getString("accountNumberLastFour");
                String routingNumber = json.getString("routingNumber");
                String accountType = json.getString("bankAccountType");

                Assert.assertNotNull(bankTokenId, "❌ BankAccountToken ID should not be null");
                Assert.assertNotNull(lastFour, "❌ Account last four digits missing");

                // Store for future tests
                ResponseStore.put("BankAccountToken", bankTokenId);
                ResponseStore.put("BankLastFourAccountNumber", lastFour);
                ResponseStore.put("BankRoutingNumber", routingNumber);
                ResponseStore.put("BankAccountType", accountType);

                Allure.step("📊 Stored Bank Token Details ➜ " +
                        "\nID: " + bankTokenId +
                        "\nLast4: " + lastFour +
                        "\nRouting: " + routingNumber +
                        "\nType: " + accountType);
            }

            // ⚠️ CLIENT ERRORS
            case 400 -> Assert.fail("❌ 400 - Bad Request: Invalid or missing field(s).");
            case 401 -> Assert.fail("❌ 401 - Unauthorized: Token invalid or expired.");
            case 403 -> Assert.fail("🚫 403 - Forbidden: Tenant permission issue.");
            case 404 -> Assert.fail("❌ 404 - Not Found: Invalid endpoint or tenant reference.");
            case 409 -> Assert.fail("⚠️ 409 - Conflict: Bank token already exists.");
            case 422 -> Assert.fail("⚠️ 422 - Validation Error: Invalid routing/account details.");

            // 🔹 SERVER ERRORS
            case 500 -> Assert.fail("💥 500 - Internal Server Error: Backend failure.");
            case 502 -> Assert.fail("💥 502 - Bad Gateway: Upstream dependency issue.");
            case 503 -> Assert.fail("🕒 503 - Service Unavailable: Try again later.");
            case 504 -> Assert.fail("⌛ 504 - Gateway Timeout: Server timed out.");

            // ⚠️ UNEXPECTED
            default -> Assert.fail("⚠️ Unexpected HTTP Status: " + status);
        }

        Allure.step("🔚 Test completed for Create Bank Payment Token API (Final Status: " + status + ")");
    }
}
