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
 * 🧩 Test Suite: Client Bank Account API
 * Validates POST /api/abcblg/bank-account — linking a bank token to a client profile.
 */
@Epic("Client Management")
@Feature("Create Client Bank Account API")
@Severity(SeverityLevel.CRITICAL)
public class CreateClientBankAccountTests {

    @Story("As a client, I can link my verified bank account token to my client profile.")
    @Test(dataProvider = "createClientBankAccountData", dataProviderClass = DataProviderUtils.class, groups = {"Regression"})
    @Description("Ensures a client bank account can be linked to an existing client profile with full HTTP status coverage.")
    public void createClientBankAccount(Map<String, Object> bankData) {

        // Step 1️⃣: Fetch client token and dependent test data
        Allure.step("🔑 Fetching client token and dependencies from ResponseStore");
        String clientToken = AuthUtils.getClientToken();
        Assert.assertTrue(clientToken != null && !clientToken.isEmpty(), "❌ Client token missing!");

        String url = ConfigManager.get("base.url") + "/api/abcblg/bank-account";
        Allure.step("🔗 Endpoint: " + url);

        // Retrieve previously stored dependent IDs
        String clientProfileId = java.util.Optional.ofNullable(ResponseStore.get("clientProfileId"))
                .map(Object::toString)
                .orElse(null);

        String bankTokenId = java.util.Optional.ofNullable(ResponseStore.get("BankAccountToken"))
                .map(Object::toString)
                .orElse(null);

        String routingNumber = java.util.Optional.ofNullable(ResponseStore.get("BankRoutingNumber"))
                .map(Object::toString)
                .orElse("");
        String accountType = java.util.Optional.ofNullable(ResponseStore.get("BankAccountType"))
                .map(Object::toString)
                .orElse("CHECKING");
        String lastFour = java.util.Optional.ofNullable(ResponseStore.get("BankLastFourAccountNumber"))
                .map(Object::toString)
                .orElse("9999");

        Assert.assertNotNull(clientProfileId, "❌ clientProfileId missing — run CreateClientProfileTests first!");
        Assert.assertNotNull(bankTokenId, "❌ BankAccountToken missing — run CreateBankPaymentTokenTests first!");

        // Step 2️⃣: Build request payload
        String clientName = "Client-" + UUID.randomUUID().toString().substring(0, 6);
        String alias = bankData.get("alias") + "-" + clientName;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientProfileId", clientProfileId);
        body.put("tokenId", bankTokenId);
        body.put("routingNumber", routingNumber);
        body.put("accountHolderName", clientName);
        body.put("type", accountType);
        body.put("lastFour", lastFour);
        body.put("email", bankData.get("email"));
        body.put("phone", bankData.get("phone"));
        body.put("alias", alias);

        String jsonBody = new GsonBuilder().setPrettyPrinting().create().toJson(body);
        Allure.addAttachment("📦 Client Bank Account Request", "application/json", jsonBody);

        // Step 3️⃣: Prepare headers (✅ FIXED TYPE-SAFE)
        String tenantId = java.util.Optional.ofNullable(ResponseStore.get("glofoxOrgId"))
                .map(Object::toString)
                .filter(s -> !s.isBlank() && !"null".equalsIgnoreCase(s))
                .orElse("41424320-4445-4620-4341-544547202020");

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + clientToken);
        headers.put("ABCFS-TENANT-ID", tenantId);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("User-Agent", "Automation-Test");

        Allure.step("🚀 Sending POST request to link Client Bank Account at: " + url);

        // Step 4️⃣: Execute POST request
        Response response = ApiClient.post(url, headers, jsonBody);
        ResponseStore.put("CreateClientBankAccountResponse", response.asString());

        // Step 5️⃣: Retry if Unauthorized
        if (response.statusCode() == 401) {
            Allure.step("⚠️ Unauthorized (401) — Retrying with refreshed token...");
            clientToken = AuthUtils.getClientToken();
            headers.put("Authorization", "Bearer " + clientToken);
            response = ApiClient.post(url, headers, jsonBody);
        }

        // Step 6️⃣: Log and attach response
        int status = response.statusCode();
        String responseBody = response.asPrettyString();
        Allure.addAttachment("📨 Client Bank Account Response", "application/json", responseBody);
        Allure.step("📥 Response Code: " + status);

        // Step 7️⃣: Handle status codes
        switch (status) {
            // ✅ SUCCESS CASES
            case 200, 201, 204 -> {
                Allure.step("✅ Bank Account linked successfully (HTTP " + status + ")");
                JsonPath json = response.jsonPath();

                String clientPaymentMethodId = json.getString("id");
                Assert.assertNotNull(clientPaymentMethodId, "❌ clientPaymentMethodId missing in response!");

                ResponseStore.put("ClientPaymentMethodId", clientPaymentMethodId);
                Allure.step("📡 Stored ClientPaymentMethodId → " + clientPaymentMethodId);
            }

            // ⚠️ CLIENT ERRORS
            case 400 -> Assert.fail("❌ 400 - Bad Request: Invalid or missing parameters.");
            case 401 -> Assert.fail("❌ 401 - Unauthorized: Token invalid or expired.");
            case 403 -> Assert.fail("🚫 403 - Forbidden: Tenant permission issue.");
            case 404 -> Assert.fail("❌ 404 - Not Found: Client or Token missing.");
            case 409 -> Assert.fail("⚠️ 409 - Conflict: Duplicate link attempt.");
            case 422 -> Assert.fail("⚠️ 422 - Validation Error: Invalid routing or account data.");

            // 🔹 SERVER ERRORS
            case 500 -> Assert.fail("💥 500 - Internal Server Error: Backend failure.");
            case 502 -> Assert.fail("💥 502 - Bad Gateway: Upstream service failure.");
            case 503 -> Assert.fail("🕒 503 - Service Unavailable: Temporary downtime.");
            case 504 -> Assert.fail("⌛ 504 - Gateway Timeout: Server response delay.");

            // ⚠️ UNEXPECTED
            default -> Assert.fail("⚠️ Unexpected HTTP Status: " + status);
        }

        Allure.step("🔚 Test completed for Create Client Bank Account API (Final Status: " + status + ")");
    }
}
