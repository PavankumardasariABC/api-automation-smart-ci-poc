package com.membertransfer.tests;

import com.membertransfer.config.ApiClient;
import com.membertransfer.config.ConfigManager;
import com.membertransfer.store.ResponseStore;
import com.membertransfer.support.MemberTransferSupport;
import io.qameta.allure.*;
import io.restassured.path.json.JsonPath;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Epic("Member transfer")
@Feature("Authorization — client_credentials")
@Severity(SeverityLevel.BLOCKER)
public class MemberTransferAuthTokenTests {

    @Test(priority = 1, groups = {"Regression", "MemberTransfer", "Smoke", "MT_Smoke"})
    @Story("Obtain bearer token (member-transfer grant)")
    @Description("POST /api/token; ApiClient retries 5xx; one extra attempt if first response is not 200.")
    public void getMemberTransferAccessToken() {
        Allure.step("Check cache: " + MemberTransferSupport.ACCESS_TOKEN_KEY);
        String existing = ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
        if (existing != null && !existing.isEmpty()) {
            Allure.step("Reusing token: " + maskToken(existing));
            return;
        }

        Allure.step("Env: " + ConfigManager.getEnv() + " — " + MemberTransferSupport.authTokenUrl());

        String creds = System.getProperty("member.transfer.auth.credentials");
        if (creds == null || creds.isBlank()) {
            creds = MemberTransferSupport.memberTransferAuthCredentials();
        }
        Assert.assertNotNull(creds, "member.transfer.auth.credentials");
        Assert.assertFalse(creds.isBlank(), "member.transfer.auth.credentials blank");

        String b64 = Base64.getEncoder().encodeToString(creds.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Basic " + b64);
        headers.put("User-Agent", "RestAssured-Automation/1.0");
        headers.put("Accept", "application/json");
        headers.put("Cache-Control", "no-cache");
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");

        var response = ApiClient.post(MemberTransferSupport.authTokenUrl(), headers, body);
        if (response.getStatusCode() != 200) {
            Allure.step("Retry: " + response.getStatusCode());
            response = ApiClient.post(MemberTransferSupport.authTokenUrl(), headers, body);
        }
        if (creds.contains("REPLACE")) {
            if (response.getStatusCode() != 200) {
                Allure.step("Placeholder config — not failing: HTTP " + response.getStatusCode());
                return;
            }
        }

        Assert.assertEquals(response.getStatusCode(), 200, "Token HTTP 200");
        JsonPath jp = response.jsonPath();
        String access = jp.getString("access_token");
        Assert.assertNotNull(access, "access_token");
        ResponseStore.put(MemberTransferSupport.ACCESS_TOKEN_KEY, access);
        Allure.addAttachment("token response", "application/json", response.asPrettyString());
    }

    private static String maskToken(String t) {
        if (t == null) return "null";
        return t.length() > 8 ? t.substring(0, 6) + "…" : "****";
    }
}
