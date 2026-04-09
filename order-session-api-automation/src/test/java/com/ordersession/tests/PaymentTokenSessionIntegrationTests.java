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

    // --- OpenAPI positive contract (201 / 200 shapes) — additive; does not change tests above ---

    @Story("POST payment token — PaymentSession 201 schema (all owner types)")
    @TmsLink("createPaymentTokenSession")
    @Test(
            dataProvider = "paymentTokenOwnerTypes",
            dataProviderClass = OrderSessionDataProvider.class,
            groups = {"OrderSession", "Regression", "Sanity"},
            priority = 4
    )
    @Description("POST /payment-session/tokens — assert PaymentSession fields per OpenAPI (201).")
    public void postPaymentTokenSession_201_matchesOpenApiPaymentSession(String ownerType) {
        if (!OrderSessionTestConfig.isPaymentTokenFlowReady()) {
            throw new SkipException("Set JWT, test.consumer.id, test.location.id");
        }

        Map<String, Object> body = OrderSessionDataProvider.validPaymentTokenBody(ownerType);
        String json = new GsonBuilder().create().toJson(body);
        Response res = OrderSessionApiService.postPaymentTokenSession(OrderSessionHeaders.jsonAuthenticated(), json);
        Assert.assertEquals(res.statusCode(), 201, res.asPrettyString());
        JsonPath jp = res.jsonPath();
        OrderSessionResponseContractAssertions.assertPaymentSessionPost201(
                jp,
                OrderSessionDataProvider.sampleConsumerId(),
                ownerType,
                OrderSessionDataProvider.sampleLocationId());
    }

    @Story("GET payment token — PaymentSessionToken schema after create")
    @TmsLink("getPaymentTokenSession")
    @Test(groups = {"OrderSession", "Regression", "Sanity"}, priority = 5)
    @Description("GET /payment-session/tokens/{id} — CREATED, CLOSED, or EXPIRED contract for a new session.")
    public void getPaymentTokenSession_200_afterCreate_matchesOpenApiContract() {
        if (!OrderSessionTestConfig.isPaymentTokenFlowReady()) {
            throw new SkipException("Set JWT, test.consumer.id, test.location.id");
        }

        Map<String, Object> body = OrderSessionDataProvider.validPaymentTokenBody("PAYOR");
        Response create = OrderSessionApiService.postPaymentTokenSession(
                OrderSessionHeaders.jsonAuthenticated(), new GsonBuilder().create().toJson(body));
        Assert.assertEquals(create.statusCode(), 201, create.asPrettyString());
        String sessionId = create.jsonPath().getString("id");

        Response get = OrderSessionApiService.getPaymentTokenSession(OrderSessionHeaders.getAuthenticated(), sessionId);
        Assert.assertEquals(get.statusCode(), 200, get.asPrettyString());
        JsonPath jp = get.jsonPath();
        String status = jp.getString("status");
        if ("CREATED".equals(status)) {
            OrderSessionResponseContractAssertions.assertPaymentSessionTokenGetCreatedState(jp, sessionId);
        } else if ("CLOSED".equals(status)) {
            OrderSessionResponseContractAssertions.assertPaymentSessionTokenGetClosedState(jp);
        } else if ("EXPIRED".equals(status)) {
            OrderSessionResponseContractAssertions.assertPaymentSessionTokenGetExpiredState(jp);
        } else {
            Assert.fail("Unexpected status after create: " + status);
        }
    }

    @Story("GET payment token — CLOSED + token (seeded session id)")
    @Test(groups = {"OrderSession", "Regression"}, priority = 6)
    @Description("GET when session is CLOSED — set " + OrderSessionTestConfig.CONFIG_PAYMENT_SESSION_CLOSED_ID + " in env.")
    public void getPaymentTokenSession_optionalClosedId_200_matchesClosedWithToken() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Bearer required");
        }
        if (!OrderSessionTestConfig.hasRealOrganizationId()) {
            throw new SkipException("Real ABCFS-ORGANIZATION-ID required");
        }
        String id = OrderSessionTestConfig.optionalPaymentSessionClosedId()
                .orElseThrow(() -> new SkipException(
                        "Set " + OrderSessionTestConfig.CONFIG_PAYMENT_SESSION_CLOSED_ID
                                + " to a CLOSED session UUID (after paypage)"));

        Response res = OrderSessionApiService.getPaymentTokenSession(OrderSessionHeaders.getAuthenticated(), id);
        Assert.assertEquals(res.statusCode(), 200, res.asPrettyString());
        OrderSessionResponseContractAssertions.assertPaymentSessionTokenGetClosedState(res.jsonPath());
    }

    @Story("GET payment token — EXPIRED (seeded session id)")
    @Test(groups = {"OrderSession", "Regression"}, priority = 7)
    @Description("GET when session is EXPIRED — set " + OrderSessionTestConfig.CONFIG_PAYMENT_SESSION_EXPIRED_ID + " in env.")
    public void getPaymentTokenSession_optionalExpiredId_200_matchesExpired() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("Bearer required");
        }
        if (!OrderSessionTestConfig.hasRealOrganizationId()) {
            throw new SkipException("Real ABCFS-ORGANIZATION-ID required");
        }
        String id = OrderSessionTestConfig.optionalPaymentSessionExpiredId()
                .orElseThrow(() -> new SkipException(
                        "Set " + OrderSessionTestConfig.CONFIG_PAYMENT_SESSION_EXPIRED_ID
                                + " to an EXPIRED session UUID"));

        Response res = OrderSessionApiService.getPaymentTokenSession(OrderSessionHeaders.getAuthenticated(), id);
        Assert.assertEquals(res.statusCode(), 200, res.asPrettyString());
        OrderSessionResponseContractAssertions.assertPaymentSessionTokenGetExpiredState(res.jsonPath());
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
