package com.membertransfer.tests;

import com.membertransfer.auth.MemberTransferAuth;
import io.qameta.allure.Allure;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

/**
 * Suite bootstrap: forwards env to logs and prefetches OAuth token when credentials are configured
 * (same idea as {@code com.ordersession.tests.BaseTestTemplate}).
 */
public class BaseTestTemplate {

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        System.out.println("Member transfer API automation — env: " + System.getProperty("env", "qa"));
        Allure.step("Suite started");
        MemberTransferAuth.prefetchBearerForSuite();
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        Allure.step("Suite finished");
        System.out.println("Allure: reports/current/");
    }
}
