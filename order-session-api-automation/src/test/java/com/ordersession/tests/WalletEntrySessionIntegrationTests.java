package com.ordersession.tests;

import com.google.gson.GsonBuilder;
import com.ordersession.dataprovider.OrderSessionDataProvider;
import com.ordersession.service.OrderSessionApiService;
import com.ordersession.store.ResponseStore;
import com.ordersession.support.OrderSessionHeaders;
import com.ordersession.support.OrderSessionResponseContractAssertions;
import com.ordersession.support.OrderSessionTestConfig;
import io.qameta.allure.*;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
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

    // --- OpenAPI positive contract (201 / 200 shapes) — additive; does not change tests above ---

    @Story("POST wallet entry — WalletEntrySession 201 full echo")
    @TmsLink("createWalletEntrySession")
    @Test(groups = {"OrderSession", "Regression", "Sanity"}, priority = 3)
    @Description("POST /payment-session/wallet-entries — optional fields echoed per OpenAPI.")
    public void postWalletEntrySession_201_fullPayload_matchesOpenApiWalletEntrySession() {
        if (!OrderSessionTestConfig.isWalletEntryFlowReady()) {
            throw new SkipException("Set wallet + payment token test.* IDs");
        }

        Map<String, Object> body = new LinkedHashMap<>(OrderSessionDataProvider.validWalletEntryBody());
        body.put("metadata", Map.of("key1", "value1"));

        String json = new GsonBuilder().create().toJson(body);
        Response res = OrderSessionApiService.postWalletEntrySession(OrderSessionHeaders.jsonAuthenticated(), json);
        Assert.assertEquals(res.statusCode(), 201, res.asPrettyString());
        OrderSessionResponseContractAssertions.assertWalletEntrySessionPost201(res.jsonPath(), body);
    }

    @Story("POST wallet entry — WalletEntrySession 201 required-only body")
    @Test(groups = {"OrderSession", "Regression"}, priority = 4)
    public void postWalletEntrySession_201_minimalRequired_matchesOpenApiWalletEntrySession() {
        if (!OrderSessionTestConfig.isWalletEntryFlowReady()) {
            throw new SkipException("Set wallet + payment token test.* IDs");
        }

        Map<String, Object> body = new LinkedHashMap<>(OrderSessionDataProvider.validWalletEntryBody());
        body.remove("paymentMethods");
        body.remove("supportedTags");

        String json = new GsonBuilder().create().toJson(body);
        Response res = OrderSessionApiService.postWalletEntrySession(OrderSessionHeaders.jsonAuthenticated(), json);
        Assert.assertEquals(res.statusCode(), 201, res.asPrettyString());
        OrderSessionResponseContractAssertions.assertWalletEntrySessionPost201(res.jsonPath(), body);
    }

    @Story("GET wallet entry — WalletEntrySession CREATED / CLOSED / EXPIRED contract")
    @TmsLink("getWalletEntrySession")
    @Test(groups = {"OrderSession", "Regression", "Sanity"}, priority = 5)
    @Description("GET after create — CREATED (no url), CLOSED + token, or EXPIRED per OpenAPI.")
    public void getWalletEntrySession_200_afterCreate_matchesOpenApiContract() {
        if (!OrderSessionTestConfig.isWalletEntryFlowReady()) {
            throw new SkipException("Wallet integration not configured");
        }

        Map<String, Object> body = OrderSessionDataProvider.validWalletEntryBody();
        Response create = OrderSessionApiService.postWalletEntrySession(
                OrderSessionHeaders.jsonAuthenticated(), new GsonBuilder().create().toJson(body));
        Assert.assertEquals(create.statusCode(), 201, create.asPrettyString());
        String sessionId = create.jsonPath().getString("id");

        Response get = OrderSessionApiService.getWalletEntrySession(OrderSessionHeaders.getAuthenticated(), sessionId);
        Assert.assertEquals(get.statusCode(), 200, get.asPrettyString());
        JsonPath jp = get.jsonPath();
        String status = jp.getString("status");
        if ("CREATED".equals(status)) {
            OrderSessionResponseContractAssertions.assertWalletEntrySessionGetCreatedState(jp, sessionId);
        } else if ("CLOSED".equals(status)) {
            OrderSessionResponseContractAssertions.assertWalletEntrySessionGetClosedState(jp);
        } else if ("EXPIRED".equals(status)) {
            OrderSessionResponseContractAssertions.assertWalletEntrySessionGetExpiredState(jp);
        } else {
            Assert.fail("Unexpected wallet session status: " + status);
        }
    }

    @Story("GET wallet entry — CLOSED + token (seeded session id)")
    @Test(groups = {"OrderSession", "Regression"}, priority = 6)
    @Description("GET when session is CLOSED — set " + OrderSessionTestConfig.CONFIG_WALLET_ENTRY_SESSION_CLOSED_ID + " in env.")
    public void getWalletEntrySession_optionalClosedId_200_matchesClosedWithToken() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Bearer required");
        }
        if (!OrderSessionTestConfig.hasRealOrganizationId()) {
            throw new SkipException("Real ABCFS-ORGANIZATION-ID required");
        }
        String id = OrderSessionTestConfig.optionalWalletEntrySessionClosedId()
                .orElseThrow(() -> new SkipException(
                        "Set " + OrderSessionTestConfig.CONFIG_WALLET_ENTRY_SESSION_CLOSED_ID
                                + " to a CLOSED wallet session UUID"));

        Response res = OrderSessionApiService.getWalletEntrySession(OrderSessionHeaders.getAuthenticated(), id);
        Assert.assertEquals(res.statusCode(), 200, res.asPrettyString());
        OrderSessionResponseContractAssertions.assertWalletEntrySessionGetClosedState(res.jsonPath());
    }
}
