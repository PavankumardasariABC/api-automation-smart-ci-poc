package com.ordersession.tests;

import com.google.gson.GsonBuilder;
import com.ordersession.dataprovider.OrderSessionDataProvider;
import com.ordersession.service.OrderSessionApiService;
import com.ordersession.auth.OrderSessionAuth;
import com.ordersession.support.ErrorPayloadAssertions;
import com.ordersession.support.OrderSessionHeaders;
import com.ordersession.support.OrderSessionTestConfig;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Security scenarios: missing bearer token. No real JWT required.
 */
@Epic("Order Session API")
@Feature("Security")
public class OrderSessionUnauthorizedTests {

    @Story("Requests without Authorization are rejected")
    @Test(groups = {"Regression", "Smoke", "OrderSession"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("POST /payment-session/tokens without Bearer → 401 and ErrorPayload")
    public void postPaymentTokenSession_withoutBearer_returns401() {
        Map<String, String> headers = OrderSessionHeaders.jsonNoAuth();
        Map<String, Object> body = OrderSessionDataProvider.validPaymentTokenBody("PAYOR");
        Response res = OrderSessionApiService.postPaymentTokenSession(headers,
                new GsonBuilder().create().toJson(body));
        Assert.assertEquals(res.statusCode(), 401, res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }

    @Story("GET session without Authorization")
    @Test(groups = {"Regression", "Smoke", "OrderSession"})
    @Severity(SeverityLevel.CRITICAL)
    public void getPaymentTokenSession_withoutBearer_returns401() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("ABCFS-ORGANIZATION-ID", OrderSessionTestConfig.organizationId());
        Response res = OrderSessionApiService.getPaymentTokenSession(headers, UUID.randomUUID().toString());
        Assert.assertEquals(res.statusCode(), 401, res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }

    @Story("POST wallet-entries without Authorization")
    @Test(groups = {"Regression", "Smoke", "OrderSession"})
    public void postWalletEntry_withoutBearer_returns401() {
        Map<String, String> headers = OrderSessionHeaders.jsonNoAuth();
        Response res = OrderSessionApiService.postWalletEntrySession(headers,
                new GsonBuilder().create().toJson(OrderSessionDataProvider.validWalletEntryBody()));
        Assert.assertEquals(res.statusCode(), 401, res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }

    @Story("GET wallet session without Authorization")
    @Test(groups = {"Regression", "Smoke", "OrderSession"})
    public void getWalletEntry_withoutBearer_returns401() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("ABCFS-ORGANIZATION-ID", OrderSessionTestConfig.organizationId());
        Response res = OrderSessionApiService.getWalletEntrySession(headers, UUID.randomUUID().toString());
        Assert.assertEquals(res.statusCode(), 401, res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }

    @Story("Missing organization header when authenticated")
    @Test(groups = {"Regression", "OrderSession"})
    @Severity(SeverityLevel.NORMAL)
    @Description("Spec: Forbidden example when ABCFS-ORGANIZATION-ID missing — may be 401/403 depending on gateway")
    public void postPaymentTokenSession_missingOrgHeader_rejected() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Set auth.bearer.token or ORDER_SESSION_JWT");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + OrderSessionAuth.bearerToken());
        headers.put("Content-Type", "application/json;charset=UTF-8");
        Map<String, Object> body = OrderSessionDataProvider.validPaymentTokenBody("PAYOR");
        Response res = OrderSessionApiService.postPaymentTokenSession(headers,
                new GsonBuilder().create().toJson(body));
        int code = res.statusCode();
        Assert.assertTrue(code == 401 || code == 403 || code == 400,
                "Expected auth/forbidden style response, got " + code + ": " + res.asPrettyString());
    }

    @Story("GET with Bearer but missing ABCFS-ORGANIZATION-ID")
    @Test(groups = {"Regression", "OrderSession"})
    @Description("Spec Forbidden example — gateway may return 401/403")
    public void getPaymentTokenSession_authenticatedMissingOrgHeader_rejected() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Set auth.bearer.token or ORDER_SESSION_JWT");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + OrderSessionAuth.bearerToken());
        Response res = OrderSessionApiService.getPaymentTokenSession(headers, UUID.randomUUID().toString());
        int code = res.statusCode();
        Assert.assertTrue(code == 401 || code == 403 || code == 400,
                "Expected auth/forbidden style response, got " + code + ": " + res.asPrettyString());
    }

    @Test(groups = {"Regression", "OrderSession"})
    public void getWalletEntrySession_authenticatedMissingOrgHeader_rejected() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Set auth.bearer.token or ORDER_SESSION_JWT");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + OrderSessionAuth.bearerToken());
        Response res = OrderSessionApiService.getWalletEntrySession(headers, UUID.randomUUID().toString());
        int code = res.statusCode();
        Assert.assertTrue(code == 401 || code == 403 || code == 400,
                "Expected auth/forbidden style response, got " + code + ": " + res.asPrettyString());
    }
}
