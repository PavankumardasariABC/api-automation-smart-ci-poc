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

    @Test(groups = {"Regression", "OrderSession"})
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

    @Test(groups = {"Regression", "OrderSession"})
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

    @Test(groups = {"Regression", "OrderSession"})
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

    @Test(groups = {"Regression", "OrderSession"})
    @Story("Invalid enum / format")
    @Description("OpenAPI OwnerTypes — invalid value should be rejected with 400 + ErrorPayload")
    public void postPaymentTokenSession_invalidOwnerType_returns400() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("JWT and org required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("consumerId", OrderSessionDataProvider.sampleConsumerId());
        body.put("ownerType", "NOT_A_VALID_OWNER_TYPE");
        body.put("locationId", OrderSessionDataProvider.sampleLocationId());
        Response res = OrderSessionApiService.postPaymentTokenSession(
                OrderSessionHeaders.jsonAuthenticated(),
                new GsonBuilder().create().toJson(body));
        Assert.assertEquals(res.statusCode(), 400, res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }

    @Test(groups = {"Regression", "OrderSession"})
    @Description("consumerId must be UUID per spec")
    public void postPaymentTokenSession_invalidConsumerIdFormat_returns400() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("JWT and org required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("consumerId", "not-a-uuid");
        body.put("ownerType", "PAYOR");
        body.put("locationId", OrderSessionDataProvider.sampleLocationId());
        Response res = OrderSessionApiService.postPaymentTokenSession(
                OrderSessionHeaders.jsonAuthenticated(),
                new GsonBuilder().create().toJson(body));
        Assert.assertEquals(res.statusCode(), 400, res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }

    @Test(groups = {"Regression", "OrderSession"})
    @Story("Malformed JSON")
    @Description("OpenAPI MALFORMED_JSON_REQUEST / MALFORMED_JSON_SYNTAX — body not valid JSON")
    public void postPaymentTokenSession_malformedJson_returns400() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("JWT and org required");
        }
        Response res = OrderSessionApiService.postPaymentTokenSession(
                OrderSessionHeaders.jsonAuthenticated(),
                "{ consumerId: no-quotes }");
        Assert.assertEquals(res.statusCode(), 400, res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }

    @Test(groups = {"Regression", "OrderSession"})
    @Description("Empty body cannot satisfy PaymentSessionBase required fields")
    public void postPaymentTokenSession_emptyJsonObject_returns400() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("JWT and org required");
        }
        Response res = OrderSessionApiService.postPaymentTokenSession(
                OrderSessionHeaders.jsonAuthenticated(),
                "{}");
        Assert.assertEquals(res.statusCode(), 400, res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }
}
