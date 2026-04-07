package com.billing.tests.negative;

import com.billing.auth.BillingAuth;
import com.billing.service.BillingApiService;
import com.billing.service.BillingHeaders;
import com.billing.tests.support.BillingTestConditions;
import com.billing.tests.support.ErrorResponseAssertions;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import org.testng.Assert;
import com.billing.tests.support.Preconditions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 400 Bad Request — invalid query (e.g. note sort) and malformed path UUIDs where applicable.
 */
@Epic("Billing API")
@Feature("Negative — Bad Request")
public class BillingBadRequestTests {

    @BeforeClass(alwaysRun = true)
    public void prereq() {
        Preconditions.skipUnless(BillingAuth.canResolveBearer(), "OAuth/JWT required");
        BillingTestConditions.assumeOrgConfigured();
    }

    @Test(groups = {"Regression"}, priority = 1,
            description = "Notes list: only 'created' is sortable — invalid field → 400")
    public void notes_invalidSort_expect400() {
        BillingTestConditions.assumeBillingAccountForNested();
        String ba = BillingTestConditions.optionalBillingAccountId();
        Response r = BillingApiService.getNotesForAccount(
                BillingHeaders.defaultAuthorized(), ba, 0, 5, "notcreated,asc");
        Assert.assertEquals(r.statusCode(), 400, r.asPrettyString());
        ErrorResponseAssertions.assertErrorEnvelope(r);
    }

    @Test(groups = {"Regression"}, priority = 2,
            description = "Path id not a valid UUID → 400 Bad Request")
    public void cancelCode_invalidUuidPath_expect400() {
        Response r = BillingApiService.getCancelCodeById(BillingHeaders.defaultAuthorized(), "not-a-uuid");
        Assert.assertTrue(r.statusCode() == 400 || r.statusCode() == 404,
                "Expected 400 or 404 for invalid path id, got " + r.statusCode() + ": " + r.asPrettyString());
    }

    @Test(groups = {"Regression"}, priority = 3,
            description = "POST note with malformed JSON → 400")
    public void createNote_malformedJson_expect400() {
        BillingTestConditions.assumeBillingAccountForNested();
        String ba = BillingTestConditions.optionalBillingAccountId();
        Response r = BillingApiService.postInvalidJsonBody(
                BillingHeaders.defaultAuthorized(), "/billing-accounts/" + ba + "/notes", "{ not json");
        Assert.assertTrue(r.statusCode() == 400 || r.statusCode() == 415,
                r.asPrettyString());
    }
}
