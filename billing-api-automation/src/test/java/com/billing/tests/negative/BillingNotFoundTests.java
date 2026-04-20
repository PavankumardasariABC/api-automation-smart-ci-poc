package com.billing.tests.negative;

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
 * 404 Not Found across path-parameter resources (OpenAPI GET by id).
 */
@Epic("Billing API")
@Feature("Negative — Not Found")
public class BillingNotFoundTests {

    private static final String FAKE_UUID = "00000000-0000-4000-8000-000000000099";

    @BeforeClass(alwaysRun = true)
    public void prereq() {
        Preconditions.skipUnless(BillingAuth.canResolveBearer(), "OAuth/JWT required");
        BillingTestConditions.assumeOrgConfigured();
    }

    @Test(groups = {"Regression"}, priority = 1)
    public void cancelCode_byId_expect404() {
        Response r = BillingApiService.getCancelCodeById(BillingHeaders.defaultAuthorized(), FAKE_UUID);
        Assert.assertEquals(r.statusCode(), 404, r.asPrettyString());
    }

    @Test(groups = {"Regression"}, priority = 2)
    public void adjustmentCode_byId_expect404() {
        Response r = BillingApiService.getAdjustmentCodeById(BillingHeaders.defaultAuthorized(), FAKE_UUID);
        Assert.assertEquals(r.statusCode(), 404, r.asPrettyString());
    }

    @Test(groups = {"Regression"}, priority = 3)
    public void billingAccount_byId_expect404() {
        Response r = BillingApiService.getBillingAccountById(BillingHeaders.defaultAuthorized(), FAKE_UUID);
        Assert.assertEquals(r.statusCode(), 404, r.asPrettyString());
    }

    @Test(groups = {"Regression"}, priority = 4)
    public void walletEntryAssignment_byId_expect404() {
        Response r = BillingApiService.getWalletEntryAssignmentById(BillingHeaders.defaultAuthorized(), FAKE_UUID);
        Assert.assertEquals(r.statusCode(), 404, r.asPrettyString());
    }
}
