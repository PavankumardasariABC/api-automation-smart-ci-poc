package com.ordersession.tests;

import com.google.gson.GsonBuilder;
import com.ordersession.dataprovider.OrderSessionDataProvider;
import com.ordersession.service.OrderSessionApiService;
import com.ordersession.store.ResponseStore;
import com.ordersession.support.OrderSessionHeaders;
import com.ordersession.support.OrderSessionTestConfig;
import io.qameta.allure.*;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.Map;

@Epic("Order Session API")
@Feature("Wallet entry session — E2E")
public class WalletEntrySessionIntegrationTests {

    @Test(groups = {"Regression", "Sanity"}, priority = 1)
    @Story("Create wallet entry session")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("createWalletEntrySession")
    public void createWalletEntrySession_returns201() {
        if (!OrderSessionTestConfig.isWalletEntryFlowReady()) {
            throw new SkipException(
                    "Set JWT, test.consumer.id, test.location.id, test.owner.id, test.wallet.user.*");
        }
        Map<String, Object> body = OrderSessionDataProvider.validWalletEntryBody();
        String json = new GsonBuilder().create().toJson(body);
        Allure.addAttachment("request", "application/json", json);

        Response res = OrderSessionApiService.postWalletEntrySession(
                OrderSessionHeaders.jsonAuthenticated(), json);
        Allure.addAttachment("response", "application/json", res.asPrettyString());

        Assert.assertEquals(res.statusCode(), 201, res.asPrettyString());
        JsonPath jp = res.jsonPath();
        Assert.assertNotNull(jp.getString("id"));
        Assert.assertEquals(jp.getString("status"), "CREATED");
        Assert.assertNotNull(jp.getString("url"));
        ResponseStore.put("LastWalletEntrySessionId", jp.getString("id"));
    }

    @Test(groups = {"Regression", "Sanity"}, priority = 2)
    @Story("GET wallet entry session does not echo hosted url")
    @TmsLink("getWalletEntrySession")
    public void getWalletEntrySession_afterCreate_returns200_withoutUrl() {
        if (!OrderSessionTestConfig.isWalletEntryFlowReady()) {
            throw new SkipException("Wallet integration not configured");
        }
        String id = (String) ResponseStore.get("LastWalletEntrySessionId");
        if (id == null) {
            throw new SkipException("Run createWalletEntrySession_returns201 first");
        }
        Response res = OrderSessionApiService.getWalletEntrySession(
                OrderSessionHeaders.getAuthenticated(), id);
        Assert.assertEquals(res.statusCode(), 200, res.asPrettyString());
        JsonPath jp = res.jsonPath();
        Assert.assertEquals(jp.getString("id"), id);
        Object url = jp.get("url");
        Assert.assertTrue(url == null || url.toString().isEmpty(),
                "OpenAPI: url only on POST create, not on GET");
    }
}
