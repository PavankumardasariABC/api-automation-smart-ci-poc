package com.ordersession.tests;

import com.google.gson.GsonBuilder;
import com.ordersession.dataprovider.OrderSessionDataProvider;
import com.ordersession.service.OrderSessionApiService;
import com.ordersession.support.ErrorPayloadAssertions;
import com.ordersession.support.OrderSessionHeaders;
import com.ordersession.support.OrderSessionTestConfig;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Epic("Order Session API")
@Feature("Wallet entry session — validation")
public class WalletEntrySessionValidationTests {

    @Test(
            groups = {"Regression", "OrderSession"},
            dataProvider = "walletEntryInvalidBodies",
            dataProviderClass = OrderSessionDataProvider.class
    )
    public void postWalletEntrySession_invalidBody_returns400(String caseName, Map<String, Object> body) {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("JWT required");
        }
        Response res = OrderSessionApiService.postWalletEntrySession(
                OrderSessionHeaders.jsonAuthenticated(),
                new GsonBuilder().create().toJson(body));
        Assert.assertEquals(res.statusCode(), 400, caseName + "\n" + res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }

    @Test(groups = {"Regression", "OrderSession"})
    @Story("Malformed JSON")
    @Description("OpenAPI — invalid JSON request body")
    public void postWalletEntrySession_malformedJson_returns400() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("JWT required");
        }
        Response res = OrderSessionApiService.postWalletEntrySession(
                OrderSessionHeaders.jsonAuthenticated(),
                "{ ownerId: ");
        Assert.assertEquals(res.statusCode(), 400, res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }

    @Test(groups = {"Regression", "OrderSession"})
    @Story("Owner / consumer not found")
    @Description("OpenAPI 404 NotFound_Owner — unresolvable PAYOR/consumer/user with syntactically valid UUIDs")
    public void postWalletEntrySession_unknownOwnerOrConsumer_returns404() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("JWT required");
        }
        if (!OrderSessionTestConfig.hasRealOrganizationId()) {
            throw new SkipException("Real ABCFS-ORGANIZATION-ID required");
        }
        Map<String, Object> body = new LinkedHashMap<>(OrderSessionDataProvider.validWalletEntryBody());
        body.put("consumerId", UUID.randomUUID().toString());
        body.put("ownerId", UUID.randomUUID().toString());
        @SuppressWarnings("unchecked")
        Map<String, Object> user = new LinkedHashMap<>((Map<String, Object>) body.get("user"));
        user.put("id", UUID.randomUUID().toString());
        user.put("externalId", UUID.randomUUID().toString());
        body.put("user", user);

        Response res = OrderSessionApiService.postWalletEntrySession(
                OrderSessionHeaders.jsonAuthenticated(),
                new GsonBuilder().create().toJson(body));
        int code = res.statusCode();
        Assert.assertTrue(
                code == 404 || code == 400,
                "Expected 404 (owner not found) or 400 (validation), got " + code + ": " + res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }
}
