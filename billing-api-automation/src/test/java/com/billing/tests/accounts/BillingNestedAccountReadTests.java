package com.billing.tests.accounts;

import com.billing.auth.BillingAuth;
import com.billing.config.ConfigManager;
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
 * Nested resources under billing-accounts/{id} — requires {@code test.billing.account.id}.
 */
@Epic("Billing API")
@Feature("Billing Accounts — Nested reads")
public class BillingNestedAccountReadTests {

    @BeforeClass(alwaysRun = true)
    public void prereq() {
        Preconditions.skipUnless(BillingAuth.canResolveBearer(), "OAuth/JWT required");
        BillingTestConditions.assumeBillingAccountForNested();
    }

    @Test(groups = {"Regression", "Sanity"}, priority = 1)
    public void subscriptions_list_expect200() {
        String ba = BillingTestConditions.optionalBillingAccountId();
        Response r = BillingApiService.getSubscriptions(BillingHeaders.defaultAuthorized(), ba, 0, 20);
        Assert.assertEquals(r.statusCode(), 200, r.asPrettyString());
    }

    @Test(groups = {"Regression"}, priority = 2)
    public void cancellations_list_expect200() {
        String ba = BillingTestConditions.optionalBillingAccountId();
        Response r = BillingApiService.getCancellationsForAccount(BillingHeaders.defaultAuthorized(), ba, 0, 10);
        Assert.assertEquals(r.statusCode(), 200, r.asPrettyString());
    }

    @Test(groups = {"Regression"}, priority = 3)
    public void resets_list_expect200() {
        String ba = BillingTestConditions.optionalBillingAccountId();
        Response r = BillingApiService.getResetsForAccount(BillingHeaders.defaultAuthorized(), ba, 0, 10);
        Assert.assertEquals(r.statusCode(), 200, r.asPrettyString());
    }

    @Test(groups = {"Regression"}, priority = 4)
    public void transfers_list_expect200() {
        String ba = BillingTestConditions.optionalBillingAccountId();
        Response r = BillingApiService.getTransfersForAccount(BillingHeaders.defaultAuthorized(), ba, 0, 10);
        Assert.assertEquals(r.statusCode(), 200, r.asPrettyString());
    }

    @Test(groups = {"Regression"}, priority = 5)
    public void notes_list_expect200() {
        String ba = BillingTestConditions.optionalBillingAccountId();
        Response r = BillingApiService.getNotesForAccount(BillingHeaders.defaultAuthorized(), ba, 0, 10, "created,desc");
        Assert.assertEquals(r.statusCode(), 200, r.asPrettyString());
    }

    @Test(groups = {"Regression"}, priority = 6)
    public void subscription_byId_whenConfigured() {
        String sub = ConfigManager.getOptional("test.subscription.id");
        Preconditions.skipUnless(sub != null && !sub.isBlank(), "test.subscription.id");
        String ba = BillingTestConditions.optionalBillingAccountId();
        Response r = BillingApiService.getSubscriptionById(BillingHeaders.defaultAuthorized(), ba, sub);
        Assert.assertTrue(r.statusCode() == 200 || r.statusCode() == 404, r.asPrettyString());
    }
}
