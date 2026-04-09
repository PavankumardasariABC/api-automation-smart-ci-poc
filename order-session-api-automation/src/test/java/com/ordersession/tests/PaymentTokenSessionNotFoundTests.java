package com.ordersession.tests;

import com.ordersession.service.OrderSessionApiService;
import com.ordersession.support.ErrorPayloadAssertions;
import com.ordersession.support.OrderSessionHeaders;
import com.ordersession.support.OrderSessionTestConfig;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.UUID;

@Epic("Order Session API")
@Feature("Payment token session — read")
public class PaymentTokenSessionNotFoundTests {

    @Test(groups = {"Regression", "Smoke", "OrderSession"})
    @Story("Unknown session id")
    @Severity(SeverityLevel.NORMAL)
    public void getPaymentTokenSession_unknownId_returns404() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("JWT required for authenticated GET");
        }
        if (!OrderSessionTestConfig.hasRealOrganizationId()) {
            throw new SkipException(
                    "Set abcfs.organization.id to a real tenant UUID; placeholder org yields 401 before 404 on GET");
        }
        Response res = OrderSessionApiService.getPaymentTokenSession(
                OrderSessionHeaders.getAuthenticated(),
                UUID.randomUUID().toString());
        Assert.assertEquals(res.statusCode(), 404, res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }

    @Test(groups = {"Regression", "OrderSession"})
    @Story("Path parameter format")
    @Description("Session id path param must be UUID — invalid format yields 400 or 404 + ErrorPayload")
    public void getPaymentTokenSession_invalidPathIdFormat_rejected() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("JWT required for authenticated GET");
        }
        if (!OrderSessionTestConfig.hasRealOrganizationId()) {
            throw new SkipException(
                    "Set abcfs.organization.id to a real tenant UUID; placeholder org yields 401 before error body");
        }
        Response res = OrderSessionApiService.getPaymentTokenSession(
                OrderSessionHeaders.getAuthenticated(),
                "not-a-valid-session-id");
        int code = res.statusCode();
        Assert.assertTrue(
                code == 400 || code == 404,
                "Expected 400 or 404 for invalid path id, got " + code + ": " + res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }
}
