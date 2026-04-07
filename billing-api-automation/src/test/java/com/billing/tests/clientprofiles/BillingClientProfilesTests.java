package com.billing.tests.clientprofiles;

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
@Feature("Client Profiles")
public class BillingClientProfilesTests {

    @BeforeClass(alwaysRun = true)
    public void prereq() {
        Preconditions.skipUnless(BillingAuth.canResolveBearer(), "OAuth/JWT required");
        BillingTestConditions.assumeOrgConfigured();
    }

    @Test(groups = {"Regression", "Smoke"}, priority = 1)
    public void clientProfiles_list_expect200() {
        Response r = BillingApiService.getClientProfiles(BillingHeaders.defaultAuthorized(), 0, 10);
        Assert.assertEquals(r.statusCode(), 200, r.asPrettyString());
        Assert.assertNotNull(r.jsonPath().get("content"));
    }

    @Test(groups = {"Regression"}, priority = 2,
            description = "Random UUID → 404 Not Found")
    public void clientProfile_byUnknownId_expect404() {
        String fake = "00000000-0000-4000-8000-000000000001";
        Response r = BillingApiService.getClientProfileById(BillingHeaders.defaultAuthorized(), fake);
        Assert.assertEquals(r.statusCode(), 404, r.asPrettyString());
    }
}
