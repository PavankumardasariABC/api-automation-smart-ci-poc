package com.membertransfer.tests;

import com.membertransfer.config.ApiClient;
import com.membertransfer.config.ConfigManager;
import com.membertransfer.store.ResponseStore;
import com.membertransfer.support.MemberTransferSupport;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.Map;

@Epic("Member transfer")
@Feature("E2E progression")
@Severity(SeverityLevel.CRITICAL)
public class MemberTransferE2EProgressionTests {

    @Test(
            priority = 1,
            groups = {"Regression", "MemberTransfer", "Progression", "E2E", "MT_Progression"}
    )
    @Description("1/3: OAuth")
    public void step01_obtainToken() {
        if (isPlaceholderData()) {
            throw new SkipException("Configure " + ConfigManager.getEnv() + " member.transfer.* and credentials");
        }
        MemberTransferSupport.fetchAndStoreToken();
        String t = (String) ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
        Assert.assertNotNull(t, "token");
        Allure.addAttachment("token (masked)", "text/plain", t != null && t.length() > 10 ? t.substring(0, 8) + "…" : "set");
    }

    @Test(
            priority = 2,
            dependsOnMethods = "step01_obtainToken",
            groups = {"Regression", "MemberTransfer", "Progression", "E2E", "MT_Progression"}
    )
    @Description("2/3: POST inquiry")
    public void step02_postInquiry() {
        if (isPlaceholderData()) {
            throw new SkipException("Config placeholders");
        }
        String t = (String) ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
        String url = MemberTransferSupport.inquiryUrl();
        String body = MemberTransferSupport.defaultInquiryBodyJson();
        Allure.addAttachment("inquiry", "application/json", body);
        Map<String, String> h = MemberTransferSupport.bearerJsonHeaders(t);
        Response r = ApiClient.post(url, h, body);
        if (r.getStatusCode() == 401) {
            MemberTransferSupport.fetchAndStoreToken();
            t = (String) ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
            r = ApiClient.post(url, MemberTransferSupport.bearerJsonHeaders(t), body);
        }
        int c = r.getStatusCode();
        if (c != 200) {
            Allure.addAttachment("inquiry error", "text/plain", r.asString());
        }
        Assert.assertEquals(c, 200, "Inquiry E2E: " + c);
        String bulk = r.jsonPath().getString("bulkId");
        Assert.assertNotNull(bulk, "bulkId");
        ResponseStore.put(MemberTransferSupport.BULK_ID_KEY, bulk);
    }

    @Test(
            priority = 3,
            dependsOnMethods = "step02_postInquiry",
            groups = {"Regression", "MemberTransfer", "Progression", "E2E", "MT_Progression"}
    )
    @Description("3/3: GET status. "
            + "Batch list GET: com.membertransfer.tests.billingaccountbatchtransfers.MemberTransferBillingAccountBatchTransfersGetApiTests")
    public void step03_getStatus() {
        if (isPlaceholderData()) {
            throw new SkipException("Config");
        }
        String bulk = ResponseStore.get(MemberTransferSupport.BULK_ID_KEY);
        if (bulk == null || bulk.isEmpty()) {
            throw new SkipException("No bulkId from step02");
        }
        String t = (String) ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
        String url = MemberTransferSupport.statusUrl(bulk);
        Allure.addAttachment("GET", "text/plain", url);
        Response r = ApiClient.get(url, MemberTransferSupport.bearerJsonHeaders(t));
        if (r.getStatusCode() == 401) {
            MemberTransferSupport.fetchAndStoreToken();
            t = (String) ResponseStore.get(MemberTransferSupport.ACCESS_TOKEN_KEY);
            r = ApiClient.get(url, MemberTransferSupport.bearerJsonHeaders(t));
        }
        int c = r.getStatusCode();
        Allure.addAttachment("status " + c, "application/json", r.asPrettyString());
        Assert.assertTrue(c >= 200 && c < 300, "2xx, was " + c);
        if (c == 200) {
            String s = r.jsonPath().getString("bulkId");
            Assert.assertNotNull(s, "bulkId in body on 200");
        }
    }

    private static boolean isPlaceholderData() {
        return contains(MemberTransferSupport::fromBillingAccountId)
                || contains(MemberTransferSupport::toLocationId)
                || contains(MemberTransferSupport::tenantId)
                || contains(() -> {
                    try {
                        return ConfigManager.getMemberTransferAuthCredentials();
                    } catch (Exception e) {
                        return "REPLACE";
                    }
                });
    }

    private static boolean contains(java.util.function.Supplier<String> s) {
        try {
            return s.get() != null && s.get().contains("REPLACE");
        } catch (Exception e) {
            return true;
        }
    }
}
