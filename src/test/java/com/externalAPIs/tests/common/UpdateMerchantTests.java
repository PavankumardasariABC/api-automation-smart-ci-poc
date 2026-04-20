package com.externalAPIs.tests.common;

import io.qameta.allure.*;
import com.externalAPIs.config.ApiClient;
import com.externalAPIs.config.ConfigManager;
import com.externalAPIs.store.ResponseStore;
import com.externalAPIs.tests.auth.AuthUtils;
import com.externalAPIs.utils.DataProviderUtils;
import com.google.gson.*;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.util.*;

/**
 * 🧩 Test Suite: Update Merchant API
 * Flow:
 *  1️⃣ Fetch full merchant JSON from ResponseStore (or via GET)
 *  2️⃣ Merge DataProvider overrides safely
 *  3️⃣ Reuse phone details from Create API
 *  4️⃣ Add processor details dynamically from ResponseStore
 *  5️⃣ PUT /merchant/{id}
 *  6️⃣ Validate full update correctness
 *  7️⃣ Compare Before vs After payloads
 */
@Epic("Merchant Management")
@Feature("Update Merchant API")
@Severity(SeverityLevel.CRITICAL)
@Link(name = "Merchant API Docs", url = "https://your-api-docs-link.com")
@Issue("MERCHANT-UPDATE-FLOW")
@TmsLink("TMS-MERCHANT-002")
public class UpdateMerchantTests {

    @Test(
            dataProvider = "updateMerchantData",
            dataProviderClass = DataProviderUtils.class,
            groups = {"Regression"},
            priority = 1,
            dependsOnMethods = {
                    "com.externalAPIs.tests.common.CreateProcessorTests.createProcessor",
                    "com.externalAPIs.tests.common.CreateMerchantTests.createMerchant"
            }
    )
    @Story("As a user, I can update existing merchant details via API")
    @Description("Validates updating merchant information and verifying that updated values persist correctly.")
    public void updateMerchant(Map<String, Object> merchantData) {

        Allure.step("1️⃣ Initialize test dependencies and authentication token");
        String token = AuthUtils.getSecureClientToken();
        String tenantId = Optional.ofNullable((String) ResponseStore.get("companyId"))
                .orElse("41424320-436f-6d70-616e-792020202020");

        String merchantId = ResponseStore.get("merchantId");
        Assert.assertNotNull(merchantId, "❌ Missing merchantId — run CreateMerchantTests first!");

        String url = ConfigManager.get("secure.base.url") + "/api/abcpg/merchant/" + merchantId;

        // Step 2️⃣: Load merchant JSON (refetch if missing)
        String fullMerchantResponse = ResponseStore.get("MerchantFullResponse");
        if (fullMerchantResponse == null || fullMerchantResponse.isBlank()) {
            Allure.step("⚠️ No MerchantFullResponse found — fetching latest merchant via GET API...");
            String fetchUrl = ConfigManager.get("secure.base.url") + "/api/abcpg/merchant/" + merchantId;
            Response getRes = ApiClient.get(fetchUrl,
                    Map.of("Authorization", "Bearer " + token,
                            "Content-Type", "application/json;charset=UTF-8",
                            "ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020"));

            // Retry if token expired
            if (getRes.statusCode() == 401) {
                Allure.step("🔑 Token expired — refreshing token for GET call");
                token = AuthUtils.getSecureClientToken();
                getRes = ApiClient.get(fetchUrl,
                        Map.of("Authorization", "Bearer " + token,
                                "Content-Type", "application/json;charset=UTF-8",
                                "ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020"));
            }

            if (getRes.statusCode() != 200)
                throw new SkipException("❌ Unable to fetch merchant details (HTTP " + getRes.statusCode() + ")");

            fullMerchantResponse = getRes.asString();
            ResponseStore.put("MerchantFullResponse", fullMerchantResponse);
            Allure.step("🔄 Refreshed merchant details from GET API");
        }

        JsonObject merchantJson = JsonParser.parseString(fullMerchantResponse).getAsJsonObject();

        // Step 3️⃣: Merge overrides from DataProvider safely
        Allure.step("🔧 Merging overrides from DataProvider");
        JsonObject inputJson = JsonParser.parseString(new Gson().toJson(merchantData)).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : inputJson.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase("id") && !entry.getKey().equalsIgnoreCase("processors")) {
                merchantJson.add(entry.getKey(), entry.getValue());
            }
        }

        // Step 4️⃣: Dynamic updates for test clarity
        long timestamp = System.currentTimeMillis();
        merchantJson.addProperty("id", merchantId);
        merchantJson.addProperty("name", merchantJson.get("name").getAsString() + " - Updated_" + timestamp);
        merchantJson.addProperty("description", "Merchant updated via automation at " + timestamp);

        // ✅ Step 5️⃣: Reuse existing phone number
        if (merchantJson.has("supportPhone")) {
            String existingPhone = merchantJson.get("supportPhone").getAsString();
            Allure.step("📞 Reusing phone number from Create API: " + existingPhone);

            JsonObject merchantSupportPhone = merchantJson.has("merchantSupportPhone")
                    ? merchantJson.getAsJsonObject("merchantSupportPhone")
                    : new JsonObject();

            merchantSupportPhone.addProperty("number", existingPhone);
            merchantSupportPhone.addProperty("countryCode", "US");
            merchantJson.add("merchantSupportPhone", merchantSupportPhone);
            merchantJson.addProperty("supportPhone", existingPhone);
        }

        // Step 6️⃣: Add processor details dynamically
        String processorId = ResponseStore.get("ProcessorId");
        String processorName = ResponseStore.get("ProcessorName");
        String paymentType = ResponseStore.get("ProcessorPaymentType");

        if (processorId != null && processorName != null && paymentType != null) {
            Allure.step("🧩 Adding processor details dynamically from ResponseStore");
            JsonObject processorObj = new JsonObject();
            processorObj.addProperty("processorId", processorId);
            processorObj.addProperty("providerSpecificMId", "364801462");
            processorObj.addProperty("isPrimary", true);
            processorObj.addProperty("name", processorName);
            processorObj.addProperty("status", "READY");
            processorObj.addProperty("paymentType", paymentType);

            JsonArray processorsArray = new JsonArray();
            processorsArray.add(processorObj);
            merchantJson.add("processors", processorsArray);
        } else {
            Allure.step("⚠️ Processor details missing — run CreateProcessorTests first!");
            throw new SkipException("Skipping UpdateMerchant as processor details are unavailable.");
        }

        // Step 7️⃣: Build request and headers
        String requestBody = new GsonBuilder().setPrettyPrinting().create().toJson(merchantJson);
        Allure.addAttachment("📦 UpdateMerchant Request", "application/json", requestBody);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("ABCFS-TENANT-ID", "41424320-436f-6d70-616e-792020202020");
        headers.put("User-Agent", "Automation-Test");

        Allure.step("🚀 Sending PUT request to update merchant");
        Response res = ApiClient.put(url, headers, requestBody);

        // Retry if token expired
        if (res.statusCode() == 401) {
            Allure.step("⚠️ Token expired — retrying with refreshed token");
            token = AuthUtils.getSecureClientToken();
            headers.put("Authorization", "Bearer " + token);
            res = ApiClient.put(url, headers, requestBody);
        }

        int status = res.statusCode();
        String responseBody = res.asPrettyString();
        Allure.addAttachment("📨 Response Body", "application/json", responseBody);

        // Step 8️⃣: Validate response
        switch (status) {
            case 200, 201, 204 -> {
                Allure.step("✅ Merchant updated successfully with HTTP " + status);
                JsonPath json = res.jsonPath();

                SoftAssert softAssert = new SoftAssert();
                String updatedMerchantId = json.getString("id");

                softAssert.assertEquals(updatedMerchantId, merchantId, "Merchant ID mismatch after update!");
                softAssert.assertTrue(json.getString("name").contains("Updated_"), "Name not updated properly!");
                softAssert.assertTrue(json.getString("description").contains("automation"), "Description not updated properly!");

                softAssert.assertEquals(json.getString("merchantSupportPhone.countryCode"), "US", "Country code mismatch!");
                String expectedPhone = merchantJson.getAsJsonObject("merchantSupportPhone").get("number").getAsString();
                softAssert.assertEquals(json.getString("merchantSupportPhone.number"), expectedPhone,
                        "❌ Phone number mismatch between Create and Update!");

                List<Map<String, Object>> processors = json.getList("processors");
                if (processors != null && !processors.isEmpty()) {
                    softAssert.assertEquals(processors.get(0).get("status"), "READY", "Processor status mismatch!");
                    softAssert.assertEquals(processors.get(0).get("paymentType"), paymentType, "Processor paymentType mismatch!");
                } else {
                    softAssert.fail("❌ Processors not found in update response!");
                }

                softAssert.assertAll();

                ResponseStore.put("MerchantFullResponse", res.asString());
                ResponseStore.put("UpdatedMerchantResponse", res.asString());
                Allure.step("✅ Merchant Updated Successfully → " + updatedMerchantId);

                // Extract and store Merchant Processor ID
                try {
                    JsonPath jsonnew = res.jsonPath();
                    List<Map<String, Object>> processorss = jsonnew.getList("processors");

                    if (processorss != null && !processors.isEmpty()) {
                        String merchantProcessorId = (String) processorss.get(0).get("id");
                        ResponseStore.put("MerchantProcessorId", merchantProcessorId);
                        Allure.step("💾 Stored MerchantProcessorId → " + merchantProcessorId);
                    } else {
                        Allure.step("⚠️ No processors found in update response to store MerchantProcessorId");
                    }
                } catch (Exception e) {
                    Allure.step("❌ Failed to extract MerchantProcessorId: " + e.getMessage());
                }

            }

            case 400 -> Assert.fail("❌ 400 - Bad Request: Input data validation failed. \n" + res.asPrettyString());
            case 401 -> Assert.fail("❌ 401 - Unauthorized: Token invalid or expired.");
            case 403 -> Assert.fail("🚫 403 - Forbidden: Insufficient permissions.");
            case 404 -> Assert.fail("❌ 404 - Not Found: Merchant ID may be invalid or deleted.");
            case 409 -> Assert.fail("⚠️ 409 - Conflict: Duplicate or invalid state.");
            case 422 -> Assert.fail("❌ 422 - Validation Error: Business rules violated.");
            case 500 -> Assert.fail("💥 500 - Internal Server Error.");
            case 502 -> Assert.fail("💥 502 - Bad Gateway.");
            case 503 -> Assert.fail("🕒 503 - Service Unavailable.");
            case 504 -> Assert.fail("⌛ 504 - Gateway Timeout.");
            default -> Assert.fail("⚠️ Unexpected Status Code: " + status);
        }

        // Step 9️⃣: Log Before vs After comparison
        Allure.addAttachment("🔍 Before vs After Update", "text/plain",
                "Before:\n" + fullMerchantResponse + "\n\nAfter:\n" + responseBody);
    }
}
