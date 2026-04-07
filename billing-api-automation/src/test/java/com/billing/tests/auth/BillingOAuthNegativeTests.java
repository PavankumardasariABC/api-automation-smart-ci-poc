package com.billing.tests.auth;

import com.billing.config.ConfigManager;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static io.restassured.RestAssured.given;

/**
 * Negative: invalid Basic credentials → 401 (per Billing API auth contract).
 */
@Epic("Billing API")
@Feature("Commerce Authorization — Negative")
public class BillingOAuthNegativeTests {

    @Test(groups = {"Regression"}, priority = 10,
            description = "Invalid client secret → 401 Unauthorized")
    public void invalidClientSecret_expect401() {
        String url = ConfigManager.get("billing.auth.url");
        String user = ConfigManager.get("billing.oauth.username");
        String encoded = Base64.getEncoder().encodeToString(
                (user + ":__INVALID_SECRET_FOR_AUTOMATION__").getBytes(StandardCharsets.UTF_8));

        Response r = given()
                .relaxedHTTPSValidation()
                .header("Authorization", "Basic " + encoded)
                .header("Accept", "application/json;charset=UTF-8")
                .contentType("application/x-www-form-urlencoded;charset=UTF-8")
                .formParam("grant_type", "client_credentials")
                .when()
                .post(url)
                .then()
                .extract()
                .response();

        int code = r.statusCode();
        Assert.assertTrue(code == 401 || code == 403,
                "Expected 401/403 for bad client credentials, got " + code + ": " + r.asPrettyString());
    }
}
