package com.billing.tests.codes;

import com.billing.auth.BillingAuth;
import com.billing.service.BillingApiService;
import com.billing.service.BillingHeaders;
import com.billing.tests.support.BillingTestConditions;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.testng.Assert;
import com.billing.tests.support.Preconditions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Codes: cancel-codes, adjustment-codes, transfer-codes — list GET 200, pagination contract.
 */
@Epic("Billing API")
@Feature("Codes")
public class BillingCodesReadTests {

    @BeforeClass(alwaysRun = true)
    public void prereq() {
        Preconditions.skipUnless(BillingAuth.canResolveBearer(), "OAuth/JWT required");
        BillingTestConditions.assumeOrgConfigured();
    }

    @Test(groups = {"Regression", "Smoke"}, priority = 1)
    public void cancelCodes_list_expect200_andPageShape() {
        Response r = BillingApiService.getCancelCodes(BillingHeaders.defaultAuthorized(), 0, 10);
        Assert.assertEquals(r.statusCode(), 200, r.asPrettyString());
        Assert.assertNotNull(r.jsonPath().get("content"));
    }

    @Test(groups = {"Regression", "Sanity"}, priority = 2)
    public void adjustmentCodes_list_expect200() {
        Response r = BillingApiService.getAdjustmentCodes(BillingHeaders.defaultAuthorized(), 0, 10);
        Assert.assertEquals(r.statusCode(), 200, r.asPrettyString());
    }

    @Test(groups = {"Regression", "Sanity"}, priority = 3)
    public void transferCodes_list_expect200() {
        Response r = BillingApiService.getTransferCodes(BillingHeaders.defaultAuthorized(), 0, 10);
        Assert.assertEquals(r.statusCode(), 200, r.asPrettyString());
    }
}
