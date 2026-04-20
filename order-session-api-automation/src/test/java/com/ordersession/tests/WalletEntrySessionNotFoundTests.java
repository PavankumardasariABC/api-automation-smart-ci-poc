package com.ordersession.tests;

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

import java.util.UUID;

@Epic("Order Session API")
@Feature("Wallet entry session — read")
public class WalletEntrySessionNotFoundTests {

    @Test(groups = {"Regression", "Smoke"})
    public void getWalletEntrySession_unknownId_returns404() {
        if (!OrderSessionTestConfig.hasValidToken()) {
            throw new SkipException("JWT required");
        }
        if (!OrderSessionTestConfig.hasRealOrganizationId()) {
            throw new SkipException(
                    "Set abcfs.organization.id to a real tenant UUID; placeholder org yields 401 before 404 on GET");
        }
        Response res = OrderSessionApiService.getWalletEntrySession(
                OrderSessionHeaders.getAuthenticated(),
                UUID.randomUUID().toString());
        Assert.assertEquals(res.statusCode(), 404, res.asPrettyString());
        ErrorPayloadAssertions.assertErrorPayloadShape(res);
    }
}
