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
 * 🧩 Test Suite: Payment Token Session API
 * Validates POST /api/abcpg/payment-session or /api/ors/payment-token/session/add-token
 * Generates consumer payment session tokens for secure ACH / Card transactions.
 */
@Epic("Payment Gateway")
@Feature("Payment Token Session API")
@Severity(SeverityLevel.BLOCKER)
@Link(name = "Payment API Docs", url = "https://your-api-docs-link.com")
@Issue("PAYMENT-TOKEN-001")
@TmsLink("TMS-PAYMENT-SESSION-001")
public class CreatePaymentTokenSessionTests {

    @Story("As a client, I can generate a secure payment token session for ACH or card transactions.")
    @Test(
            priority = 1,
            dataProvider = "createPaymentTokenSessionData",
            dataProviderClass = DataProviderUtils.class,
            groups = {"Regression", "Sanity"}
    )
    @Description("Validates creation of a consumer payment token session using Glofox Client Token and organization context.")
    public void createPaymentTokenSession(Map<String, Object> requestData) {

        // Step 1️⃣ Retrieve authentication token
        Allure.step("🔑 Fetching Glofox Client Token for Payment Session...");
        String token = Optional.ofNullable(ResponseStore.get("GlofoxClientToken"))
                .map(Object::toString)
                .filter(s -> !s.isBlank() && !"null".equalsIgnoreCase(s))
                .orElseGet(AuthUtils::getGlofoxClientToken);

        Assert.assertNotNull(token, "❌ Missing Glofox Client Token!");
        Allure.step("✅ Token fetched successfully");

        // Step 2️⃣ Determine Tenant ID
        String tenantId = Optional.ofNullable(ResponseStore.get("glofoxOrgId"))
                .map(Object::toString)
                .filter(s -> !s.isBlank() && !"null".equalsIgnoreCase(s))
                .orElse("76e18132-a304-44bd-a524-6641beba7b57"); // ✅ Default Tenant ID from Postman

        Allure.step("🏢 Using Tenant ID: " + tenantId);

        // Step 3️⃣ Clean Base URL and select endpoint
        String baseUrl = ConfigManager.get("base.url").replaceAll("/$", ""); // remove trailing slash
        String endpoint = baseUrl.contains("30preprod")
                ? "/api/ors/payment-token/session/add-token"
                : "/api/abcpg/payment-session";

        String url = baseUrl + endpoint;
        Allure.step("🔗 Target Endpoint: " + url);

        // Step 4️⃣ Build Headers
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("ABCFS-TENANT-ID", tenantId);
        headers.put("User-Agent", "Automation-Test");

        Allure.addAttachment("📋 Request Headers", "application/json",
                new GsonBuilder().setPrettyPrinting().create().toJson(headers));

        // Step 5️⃣ Build request body with required fields only
        String consumerId = Optional.ofNullable(ResponseStore.get("PaymentSessionConsumerId"))
                .map(Object::toString)
                .filter(s -> !s.isBlank() && !"null".equalsIgnoreCase(s))
                .orElse((String) requestData.getOrDefault("consumerId", null));

        Assert.assertNotNull(consumerId, "❌ Consumer ID is required but not found in ResponseStore or DataProvider!");
        requestData.put("consumerId", consumerId);

        // ✅ Only required fields as per working Postman payload
        requestData.putIfAbsent("metadata", Map.of("limitedPrimaryPaymentMethod", "ANY"));
        requestData.putIfAbsent("consumerOrigin", "https://dev.30preprod.com");
        requestData.putIfAbsent("cvvRequired", false);
        requestData.putIfAbsent("ownerType", "PAYOR");

        // Step 6️⃣ Attach request for reporting
        String requestBody = new GsonBuilder().setPrettyPrinting().create().toJson(requestData);
        Allure.addAttachment("📦 Payment Token Session Request", "application/json", requestBody);

        // Step 7️⃣ Execute API request
        Allure.step("🚀 Sending POST request to Payment Token Session API...");
        Response res = ApiClient.post(url, headers, requestBody);
        ResponseStore.put("CreatePaymentSessionRawResponse", res.asString());

        // Retry if unauthorized
        if (res.statusCode() == 401) {
            Allure.step("⚠️ 401 Unauthorized — retrying with refreshed Glofox Client Token...");
            token = AuthUtils.getGlofoxClientToken();
            headers.put("Authorization", "Bearer " + token);
            res = ApiClient.post(url, headers, requestBody);
        }

        int status = res.statusCode();
        String responseBody = res.asPrettyString();
        Allure.addAttachment("📨 Payment Token Session Response", "application/json", responseBody);
        Allure.step("📥 HTTP Status: " + status);

        // Step 8️⃣: Validate responses
        switch (status) {
            case 200, 201, 204 -> {
                Allure.step("✅ Payment Token Session created successfully (HTTP " + status + ")");
                JsonPath json = res.jsonPath();

                String sessionId = json.getString("id");
                String consumerIdResp = json.getString("consumerId");
                String statusMsg = json.getString("status");

                Assert.assertNotNull(sessionId, "❌ Session ID missing!");
                Assert.assertNotNull(consumerIdResp, "❌ Consumer ID missing!");

                // Store for dependent APIs
                ResponseStore.put("PaymentSessionId", sessionId);
                ResponseStore.put("PaymentSessionConsumerId", consumerIdResp);
                ResponseStore.put("PaymentSessionFullResponse", res.asString());

                Allure.step("📊 Stored Session Info → " +
                        "\n🆔 Session ID: " + sessionId +
                        "\n👤 Consumer ID: " + consumerIdResp +
                        "\n📘 Status: " + statusMsg);
            }

            case 400 -> Assert.fail("❌ 400 - Bad Request: Invalid or missing field.\n" + responseBody);
            case 401 -> Assert.fail("❌ 401 - Unauthorized: Token invalid or expired.");
            case 403 -> Assert.fail("🚫 403 - Forbidden: Insufficient permissions for tenant.");
            case 404 -> Assert.fail("❌ 404 - Not Found: Consumer or endpoint missing.");
            case 409 -> Assert.fail("⚠️ 409 - Conflict: Duplicate payment session detected.");
            case 422 -> Assert.fail("⚠️ 422 - Validation Error: Invalid consumer/payment details.");
            case 500 -> Assert.fail("💥 500 - Internal Server Error — Check tenant ID, token scope, or missing required fields.\nResponse:\n" + responseBody);
            case 502 -> Assert.fail("💥 502 - Bad Gateway: Upstream dependency failure.");
            case 503 -> Assert.fail("🕒 503 - Service Unavailable.");
            case 504 -> Assert.fail("⌛ 504 - Gateway Timeout.");
            default -> Assert.fail("⚠️ Unexpected HTTP Status: " + status);
        }

        Allure.step("🏁 Completed Create Payment Token Session Test Successfully.");
    }
}
