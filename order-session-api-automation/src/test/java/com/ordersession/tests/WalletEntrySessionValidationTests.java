package com.ordersession.tests;

import com.google.gson.GsonBuilder;
import com.ordersession.dataprovider.OrderSessionDataProvider;
import com.ordersession.service.OrderSessionApiService;
import com.ordersession.support.ErrorPayloadAssertions;
import com.ordersession.support.OrderSessionHeaders;
import com.ordersession.support.OrderSessionTestConfig;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.Map;

@Epic("Order Session API")
@Feature("Wallet entry session — validation")
public class WalletEntrySessionValidationTests {

    @Test(
            groups = {"Regression"},
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
}
