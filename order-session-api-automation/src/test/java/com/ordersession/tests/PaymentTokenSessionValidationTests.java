package com.ordersession.tests;

import com.google.gson.GsonBuilder;
import com.ordersession.dataprovider.OrderSessionDataProvider;
import com.ordersession.service.OrderSessionApiService;
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

@Epic("Order Session API")
@Feature("Payment token session — validation")
public class PaymentTokenSessionValidationTests {

    @Test(groups = {"Regression"})
    @Story("Missing required fields")
    @Severity(SeverityLevel.NORMAL)
    public void postPaymentTokenSession_missingConsumerId_returns400() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("JWT and org required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ownerType", "PAYOR");
        body.put("locationId", OrderSessionDataProvider.sampleLocationId());
        Response res = OrderSessionApiService.postPaymentTokenSession(
                OrderSessionHeaders.jsonAuthenticated(),
                new GsonBuilder().create().toJson(body));
        Assert.assertEquals(res.statusCode(), 400, res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }

    @Test(groups = {"Regression"})
    public void postPaymentTokenSession_missingOwnerType_returns400() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("JWT and org required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("consumerId", OrderSessionDataProvider.sampleConsumerId());
        body.put("locationId", OrderSessionDataProvider.sampleLocationId());
        Response res = OrderSessionApiService.postPaymentTokenSession(
                OrderSessionHeaders.jsonAuthenticated(),
                new GsonBuilder().create().toJson(body));
        Assert.assertEquals(res.statusCode(), 400, res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }

    @Test(groups = {"Regression"})
    public void postPaymentTokenSession_missingLocationId_returns400() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("JWT and org required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("consumerId", OrderSessionDataProvider.sampleConsumerId());
        body.put("ownerType", "PAYOR");
        Response res = OrderSessionApiService.postPaymentTokenSession(
                OrderSessionHeaders.jsonAuthenticated(),
                new GsonBuilder().create().toJson(body));
        Assert.assertEquals(res.statusCode(), 400, res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }
}
