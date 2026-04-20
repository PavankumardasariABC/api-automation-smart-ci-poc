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
@Feature("Payment token session — E2E")
public class PaymentTokenSessionIntegrationTests {

    @Test(groups = {"Regression", "Sanity"}, priority = 1)
    @Story("Create PAYOR token session")
    @Severity(SeverityLevel.BLOCKER)
    @TmsLink("createPaymentTokenSession")
    public void createPaymentTokenSession_payor_returns201() {
        if (!OrderSessionTestConfig.isPaymentTokenFlowReady()) {
            throw new SkipException("Set JWT, test.consumer.id, test.location.id in env properties");
        }
        assertCreate201("PAYOR", "LastPaymentSessionId_PAYOR");
    }

    @Test(groups = {"Regression", "Sanity"}, priority = 2)
    @Story("Retrieve PAYOR session after create")
    @TmsLink("getPaymentTokenSession")
    public void getPaymentTokenSession_afterPayorCreate_returns200() {
        if (!OrderSessionTestConfig.isPaymentTokenFlowReady()) {
            throw new SkipException("Integration data not configured");
        }
        String id = (String) ResponseStore.get("LastPaymentSessionId_PAYOR");
        if (id == null) {
            throw new SkipException("Run createPaymentTokenSession_payor_returns201 first");
        }
        Response res = OrderSessionApiService.getPaymentTokenSession(
                OrderSessionHeaders.getAuthenticated(), id);
        Assert.assertEquals(res.statusCode(), 200, res.asPrettyString());
        JsonPath jp = res.jsonPath();
        Assert.assertEquals(jp.getString("id"), id);
        String status = jp.getString("status");
        Assert.assertTrue("CREATED".equals(status) || "CLOSED".equals(status) || "EXPIRED".equals(status),
                "Unexpected status: " + status);
    }

    @Test(
            groups = {"Regression"},
            priority = 3,
            dataProvider = "locationAndMemberOwnerTypes",
            dataProviderClass = com.ordersession.dataprovider.OrderSessionDataProvider.class
    )
    @Story("Create token session for LOCATION and MEMBER")
    public void createPaymentTokenSession_locationOrMember_returns201(String ownerType) {
        if (!OrderSessionTestConfig.isPaymentTokenFlowReady()) {
            throw new SkipException("Set JWT, test.consumer.id, test.location.id in env properties");
        }
        assertCreate201(ownerType, "LastPaymentSessionId_" + ownerType);
    }

    private void assertCreate201(String ownerType, String storeKey) {
        Map<String, Object> body = OrderSessionDataProvider.validPaymentTokenBody(ownerType);
        String json = new GsonBuilder().create().toJson(body);
        Allure.addAttachment("request", "application/json", json);

        Response res = OrderSessionApiService.postPaymentTokenSession(
                OrderSessionHeaders.jsonAuthenticated(), json);

        Allure.addAttachment("response", "application/json", res.asPrettyString());
        Assert.assertEquals(res.statusCode(), 201, res.asPrettyString());

        JsonPath jp = res.jsonPath();
        Assert.assertNotNull(jp.getString("id"));
        Assert.assertEquals(jp.getString("consumerId"), OrderSessionDataProvider.sampleConsumerId());
        Assert.assertEquals(jp.getString("ownerType"), ownerType);
        Assert.assertEquals(jp.getString("status"), "CREATED");
        Assert.assertNotNull(jp.getString("url"));

        ResponseStore.put(storeKey, jp.getString("id"));
    }
}
