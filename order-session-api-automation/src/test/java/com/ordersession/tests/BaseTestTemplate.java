package com.ordersession.tests;

import com.ordersession.auth.OrderSessionAuth;
import io.qameta.allure.Allure;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

public class BaseTestTemplate {

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        System.out.println("Order Session API automation — env: " + System.getProperty("env", "qa"));
        Allure.step("Suite started");
        // Authorization API first: cache bearer in ResponseStore for every headersWithBearer(...) call
        OrderSessionAuth.prefetchBearerForSuite();
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        Allure.step("Suite finished");
        System.out.println("Reports: reports/current/");
    }
}
