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

@Epic("Billing API")
@Feature("Billing Accounts — Read")
public class BillingAccountsReadTests {

    @BeforeClass(alwaysRun = true)
    public void prereq() {
        Preconditions.skipUnless(BillingAuth.canResolveBearer(), "OAuth/JWT required");
        BillingTestConditions.assumeOrgConfigured();
    }

    @Test(groups = {"Regression", "Smoke", "Sanity"}, priority = 1)
    public void billingAccounts_list_expect200() {
        Response r = BillingApiService.getBillingAccounts(BillingHeaders.defaultAuthorized(), 0, 10, null);
        Assert.assertEquals(r.statusCode(), 200, r.asPrettyString());
        Assert.assertNotNull(r.jsonPath().get("content"));
    }

    @Test(groups = {"Regression"}, priority = 2)
    public void billingAccounts_list_withOptionalExternalIdQuery() {
        String ext = ConfigManager.getOptional("test.external.id");
        Preconditions.skipUnless(ext != null && !ext.isBlank(), "test.external.id optional");
        String url = ConfigManager.get("billing.base.url").replaceAll("/$", "")
                + "/billing-accounts?page=0&size=5&externalId=" + ext;
        Response r = com.billing.config.ApiClient.get(url, BillingHeaders.defaultAuthorized());
        Assert.assertTrue(r.statusCode() == 200 || r.statusCode() == 404,
                "List by externalId: " + r.statusCode() + " " + r.asPrettyString());
    }
}
