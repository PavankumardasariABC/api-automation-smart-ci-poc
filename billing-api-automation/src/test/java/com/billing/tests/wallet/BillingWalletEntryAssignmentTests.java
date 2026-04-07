package com.billing.tests.wallet;

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

@Epic("Billing API")
@Feature("Wallet Entry Assignments")
public class BillingWalletEntryAssignmentTests {

    @BeforeClass(alwaysRun = true)
    public void prereq() {
        Preconditions.skipUnless(BillingAuth.canResolveBearer(), "OAuth/JWT required");
        BillingTestConditions.assumeOrgConfigured();
    }

    @Test(groups = {"Regression"}, priority = 1)
    public void walletEntryAssignments_list_expect200() {
        Response r = BillingApiService.getWalletEntryAssignments(BillingHeaders.defaultAuthorized(), 0, 10);
        Assert.assertEquals(r.statusCode(), 200, r.asPrettyString());
    }
}
