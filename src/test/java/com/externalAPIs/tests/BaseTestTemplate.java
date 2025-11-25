package com.externalAPIs.tests;

import io.qameta.allure.Allure;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.AfterSuite;

/**
 * BaseTestTemplate
 * -------------------------------------------------------
 * Handles suite-level setup and teardown.
 * Replaces old ExtentReports TestListener with Allure steps.
 */
public class BaseTestTemplate {

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        System.out.println("🔧 Starting External API Test Suite");
        Allure.step("🔧 Test Suite Started");
       // System.out.println("Environment: " + System.getProperty("env", "dev"));
        System.out.println("Environment: " + System.getProperty("env", "qa"));
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        System.out.println("📊 External API Test Suite Completed");
        Allure.step("📊 API Test Suite Completed");
        System.out.println("➡️ Report generated at: /reports/current/");
    }
}
