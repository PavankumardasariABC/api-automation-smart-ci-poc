package com.billing.tests;

import com.billing.auth.BillingAuth;
import io.qameta.allure.Allure;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

public class BaseTestTemplate {

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        System.out.println("Billing API automation — env: " + System.getProperty("env", "qa"));
        Allure.step("Suite started — prefetch Commerce bearer for Billing API");
        BillingAuth.prefetchBearerForSuite();
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        Allure.step("Suite finished");
        System.out.println("Reports: billing-api-automation/reports/current/");
    }
}
