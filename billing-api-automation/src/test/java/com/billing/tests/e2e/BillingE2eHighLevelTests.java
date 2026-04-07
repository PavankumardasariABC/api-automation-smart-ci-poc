package com.billing.tests.e2e;

import com.billing.auth.BillingAuth;
import com.billing.service.BillingApiService;
import com.billing.service.BillingHeaders;
import com.billing.tests.support.BillingTestConditions;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import com.billing.tests.support.Preconditions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * High-level E2E: authorize → read reference data → read billing accounts → wallet assignments.
 */
@Epic("Billing API")
@Feature("E2E — High level")
public class BillingE2eHighLevelTests {

    @BeforeClass(alwaysRun = true)
    public void prereq() {
        Preconditions.skipUnless(BillingAuth.canResolveBearer(), "OAuth/JWT required");
        BillingTestConditions.assumeOrgConfigured();
    }

    @Story("End-to-end read journey after Commerce OAuth")
    @Test(groups = {"Regression", "Sanity"}, priority = 1,
            description = "Token reuse across cancel codes, adjustment codes, transfer codes, client profiles, billing accounts, wallet")
    public void e2e_readJourney_all200() {
        var h = BillingHeaders.defaultAuthorized();

        Response cancel = BillingApiService.getCancelCodes(h, 0, 5);
        Assert.assertEquals(cancel.statusCode(), 200, cancel.asPrettyString());

        Response adj = BillingApiService.getAdjustmentCodes(h, 0, 5);
        Assert.assertEquals(adj.statusCode(), 200, adj.asPrettyString());

        Response tr = BillingApiService.getTransferCodes(h, 0, 5);
        Assert.assertEquals(tr.statusCode(), 200, tr.asPrettyString());

        Response cp = BillingApiService.getClientProfiles(h, 0, 5);
        Assert.assertEquals(cp.statusCode(), 200, cp.asPrettyString());

        Response ba = BillingApiService.getBillingAccounts(h, 0, 5, null);
        Assert.assertEquals(ba.statusCode(), 200, ba.asPrettyString());

        Response we = BillingApiService.getWalletEntryAssignments(h, 0, 5);
        Assert.assertEquals(we.statusCode(), 200, we.asPrettyString());

        Assert.assertFalse(BillingAuth.bearerToken().isBlank(), "Bearer still available after chain");
    }
}
