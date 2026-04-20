package com.externalAPIs.tests.common;
import io.qameta.allure.Description;
import com.externalAPIs.config.ApiClient;
import com.externalAPIs.config.ConfigManager;
import com.externalAPIs.store.ResponseStore;
import com.externalAPIs.tests.auth.AuthUtils;
import com.externalAPIs.utils.DataProviderUtils;
import com.google.gson.GsonBuilder;
import io.qameta.allure.*;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * 🧩 Test Suite: Create Payment Session Consumer API
 * Tests POST /api/ors/payment-session-consumer with Allure-based reporting.
 */
@Epic("Payment Session Management")
@Feature("Create Payment Session Consumer API")
@Severity(SeverityLevel.CRITICAL)
public class CreatePaymentSessionConsumerTests {

    @Story("As a user, I can create a new payment session consumer via API")
    @Test(dataProvider = "createPaymentSessionConsumerData", dataProviderClass = DataProviderUtils.class, groups = {"Regression"})
    @Description("Validates creation of a payment session consumer and ensures response structure and status validation.")

    public void createPaymentSessionConsumer(Map<String, Object> consumerData) {

        Allure.step("1️⃣ Retrieve Client Token (non-secure)");
        String token = AuthUtils.getClientToken();

        // Step 2️⃣: Construct URL
        String url = ConfigManager.get("base.url") + "/api/ors/payment-session-consumer";

        // Step 3️⃣: Headers
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + token,
                "Content-Type", "application/json;charset=UTF-8",
                "User-Agent", "Automation-Test"
        );

        // Step 4️⃣: Prepare Request Body
        String body = new GsonBuilder().setPrettyPrinting().create().toJson(consumerData);
        Allure.addAttachment("📦 Payment Session Consumer Request", "application/json", body);

        Allure.step("🚀 Sending POST request to create Payment Session Consumer");
        Response res = ApiClient.post(url, headers, body);
        ResponseStore.put("CreatePaymentSessionConsumerResponse", res.asString());

        // Step 5️⃣: Retry if Unauthorized
        if (res.statusCode() == 401) {
            Allure.step("⚠️ Unauthorized! Retrying with a fresh token...");
            token = AuthUtils.getClientToken();
            res = ApiClient.post(url, headers, body);
        }

        // Step 6️⃣: Capture Response
        int status = res.statusCode();
        String responseBody = res.asPrettyString();
        Allure.addAttachment("📨 Payment Session Consumer Response", "application/json", responseBody);

        // Step 7️⃣: Validate and handle responses
        switch (status) {

            // ✅ Success
            case 200, 201 -> {
                Allure.step("✅ Payment Session Consumer created successfully with status " + status);
                JsonPath json = res.jsonPath();
                String consumerId = json.getString("id");

                Assert.assertNotNull(consumerId, "Consumer ID should not be null");
                ResponseStore.put("PaymentSessionConsumerId", consumerId);

                Allure.step("📡 Consumer Created → ID: " + consumerId);
            }

            // 🔹 Client errors
            case 400 -> {
                Allure.step("❌ Bad Request (400) — Invalid or missing fields in request body.");
                Assert.fail("400 - Bad Request: Invalid input fields");
            }

            case 401 -> {
                Allure.step("❌ Unauthorized (401) — Token expired or invalid.");
                Assert.fail("401 - Unauthorized");
            }

            case 403 -> {
                Allure.step("🚫 Forbidden (403) — Access denied for this user or environment.");
                Assert.fail("403 - Forbidden");
            }

            case 404 -> {
                Allure.step("❌ Not Found (404) — Endpoint or linked resource missing.");
                Assert.fail("404 - Not Found");
            }

            case 409 -> {
                Allure.step("⚠️ Conflict (409) — Consumer already exists with same name.");
                // skip, not fail
            }

            case 422 -> {
                Allure.step("❌ Validation Error (422) — Invalid field or missing parameter.");
                Assert.fail("422 - Validation Error");
            }

            // ⚠️ Unexpected
            default -> {
                Allure.step("⚠️ Unexpected Status: " + status + " — Verify API response contract.");
                Assert.fail("Unexpected Status Code: " + status);
            }
        }
    }
}
