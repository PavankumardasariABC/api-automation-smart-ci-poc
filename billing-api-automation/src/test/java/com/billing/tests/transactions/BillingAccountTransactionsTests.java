package com.billing.tests.transactions;

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
 * Transactions require {@code ABCFS-LOCATION-ID} per OpenAPI.
 */
@Epic("Billing API")
@Feature("Billing Account Transactions")
public class BillingAccountTransactionsTests {

    @BeforeClass(alwaysRun = true)
    public void prereq() {
        Preconditions.skipUnless(BillingAuth.canResolveBearer(), "OAuth/JWT required");
        BillingTestConditions.assumeBillingAccountForNested();
    }

    @Test(groups = {"Regression"}, priority = 1,
            description = "Without location header → 400 (BadRequest_GetTransaction)")
    public void transactions_missingLocation_expect400() {
        Preconditions.skipIf(BillingTestConditions.hasLocationHeader(),
                "Skip when billing.location.id is set (positive case covered in other test)");
        String ba = BillingTestConditions.optionalBillingAccountId();
        Response r = BillingApiService.getBillingAccountTransactions(
                BillingHeaders.defaultAuthorized(), ba, 0, 10);
        Assert.assertEquals(r.statusCode(), 400, r.asPrettyString());
    }

    @Test(groups = {"Regression"}, priority = 2,
            description = "With location header → 200 when account exists")
    public void transactions_withLocation_expect200() {
        Preconditions.skipUnless(BillingTestConditions.hasLocationHeader(), "Set billing.location.id");
        String ba = BillingTestConditions.optionalBillingAccountId();
        Response r = BillingApiService.getBillingAccountTransactions(
                BillingHeaders.bearerJsonWithLocation(BillingAuth.bearerToken()), ba, 0, 10);
        Assert.assertTrue(r.statusCode() == 200 || r.statusCode() == 404,
                r.asPrettyString());
    }
}
