package com.billing.tests.security;

import com.billing.service.BillingApiService;
import com.billing.service.BillingHeaders;
import com.billing.tests.support.BillingTestConditions;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 401 when Authorization is missing or not a valid bearer (OpenAPI security: bearerAuth).
 */
@Epic("Billing API")
@Feature("Security — Unauthorized")
public class BillingUnauthorizedTests {

    @Test(groups = {"Regression"}, priority = 5,
            description = "GET cancel-codes without Bearer → 401")
    public void cancelCodes_missingBearer_expect401() {
        BillingTestConditions.assumeOrgConfigured();
        Response r = BillingApiService.getCancelCodes(BillingHeaders.missingAuthButOrg(), 0, 5);
        Assert.assertEquals(r.statusCode(), 401, r.asPrettyString());
    }

    @Test(groups = {"Regression"}, priority = 6,
            description = "GET cancel-codes with invalid Bearer → 401")
    public void cancelCodes_invalidBearer_expect401() {
        BillingTestConditions.assumeOrgConfigured();
        Response r = BillingApiService.getCancelCodes(BillingHeaders.invalidBearer(), 0, 5);
        Assert.assertEquals(r.statusCode(), 401, r.asPrettyString());
    }
}
