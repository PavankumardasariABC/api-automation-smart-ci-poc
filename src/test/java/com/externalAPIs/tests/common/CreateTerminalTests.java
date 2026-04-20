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
 * 🧩 Test Suite: Create Terminal API
 * Validates POST /api/abcpg/terminal using Allure-based structured reporting.
 */
@Epic("Terminal Management")
@Feature("Create Terminal API")
@Severity(SeverityLevel.CRITICAL)
@Link(name = "Terminal API Docs", url = "https://your-api-docs-link.com")
@Issue("TERMINAL-CREATE-001")
@TmsLink("TMS-TERMINAL-001")
public class CreateTerminalTests {

    @Story("As an admin, I can create a terminal via API")
    @Test(
            dataProvider = "createTerminalData",
            dataProviderClass = DataProviderUtils.class,
            groups = {"Regression"},
            priority = 1,
            dependsOnMethods = {
                    "com.externalAPIs.tests.common.CreateProcessorTests.createProcessor",
                    "com.externalAPIs.tests.common.UpdateMerchantTests.updateMerchant"
            }
    )
    @Description("Validates creation of a new terminal with unique terminal number and linked processor details dynamically.")
    public void createTerminal(Map<String, Object> terminalData) throws InterruptedException {

        Allure.step("1️⃣ Retrieve authentication token and dependencies");
        String token = AuthUtils.getSecureClientToken();
        Assert.assertNotNull(token, "❌ Secure client token is null!");

        // ✅ Fixed Tenant ID (default always)
        String tenantId = "41424320-436f-6d70-616e-792020202020";
        Allure.step("🏢 Using Default Tenant ID: " + tenantId);

        // ✅ Fetch Processor ID dynamically (from Update Merchant or Create Processor)
        String processorId = Optional.ofNullable(ResponseStore.get("MerchantProcessorId"))
                .map(Object::toString)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new AssertionError("❌ Missing MerchantProcessorId in ResponseStore — run UpdateMerchant or CreateProcessor first!"));

        Allure.step("🔗 Using Merchant Processor ID: " + processorId);

        // Inject runtime Processor ID & unique terminal number
        terminalData.put("merchantProcessorId", processorId);
        String uniqueNumber = "qa_terminal_" + System.currentTimeMillis();
        terminalData.put("terminalNumber", uniqueNumber);

        if (terminalData.containsKey("name")) {
            terminalData.put("name", terminalData.get("name") + "_" + System.currentTimeMillis());
        }

        Allure.step("🔧 Updated payload with runtime values (terminal number, name, processor)");

        // ✅ Construct URL safely
        String baseUrl = ConfigManager.get("secure.base.url").replaceAll("/$", "");
        String url = baseUrl + "/api/abcpg/terminal";

        // ✅ Build headers
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("ABCFS-TENANT-ID", tenantId);
        headers.put("User-Agent", "Automation-Test");

        // ✅ Prepare request body
        String body = new GsonBuilder().setPrettyPrinting().create().toJson(terminalData);
        Allure.addAttachment("📦 Create Terminal Request", "application/json", body);

        // 🚀 Execute API
        Allure.step("🚀 Sending POST request to create terminal...");
        Response res = ApiClient.post(url, headers, body);
        ResponseStore.put("CreateTerminalRawResponse", res.asString());

        // 🔁 Retry for 401 (token expiry)
        if (res.statusCode() == 401) {
            Allure.step("🔑 Token expired — retrying with refreshed token...");
            token = AuthUtils.getSecureClientToken();
            headers.put("Authorization", "Bearer " + token);
            res = ApiClient.post(url, headers, body);
        }

        // 🔁 Retry for transient 404 (processor not yet propagated)
        if (res.statusCode() == 404) {
            Allure.step("⚠️ 404 received — retrying once after 3 seconds (possible async processor propagation)...");
            Thread.sleep(3000);
            res = ApiClient.post(url, headers, body);
        }

        // 🧾 Log final response
        int status = res.statusCode();
        String responseBody = res.asPrettyString();
        Allure.addAttachment("📨 Terminal API Response", "application/json", responseBody);

        System.out.println("📥 HTTP Status: " + status);
        System.out.println("📨 Response: " + responseBody);

        // ✅ Validate response
        switch (status) {
            case 200, 201 -> {
                Allure.step("✅ Terminal created successfully (HTTP " + status + ")");
                JsonPath json = res.jsonPath();

                String terminalId = json.getString("id");
                String terminalNumber = json.getString("terminalNumber");

                Assert.assertNotNull(terminalId, "❌ Terminal ID should not be null");
                Assert.assertNotNull(terminalNumber, "❌ Terminal Number should not be null");

                ResponseStore.put("TerminalId", terminalId);
                ResponseStore.put("TerminalNumber", terminalNumber);
                ResponseStore.put("TerminalFullResponse", res.asString());

                Allure.step("📡 Terminal Created → ID: " + terminalId + " | Number: " + terminalNumber);
            }

            case 409 -> Allure.step("⚠️ Conflict (409) — Terminal already exists with same number.");

            case 400 -> Assert.fail("❌ 400 - Bad Request:\n" + responseBody);
            case 401 -> Assert.fail("❌ 401 - Unauthorized: Token invalid or expired.");
            case 403 -> Assert.fail("🚫 403 - Forbidden: Access denied for tenant.");
            case 404 -> Assert.fail("❌ 404 - Not Found: Processor or Org ID missing.\nEnsure correct chaining from UpdateMerchant.");
            case 422 -> Assert.fail("⚠️ 422 - Validation Error:\n" + responseBody);
            case 500 -> Assert.fail("💥 500 - Internal Server Error:\n" + responseBody);
            default -> Assert.fail("⚠️ Unexpected HTTP Status: " + status + "\n" + responseBody);
        }

        Allure.step("🏁 Terminal API test completed — details stored for dependent APIs.");
    }
}
