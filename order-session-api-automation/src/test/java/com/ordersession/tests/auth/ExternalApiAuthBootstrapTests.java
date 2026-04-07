package com.ordersession.tests.auth;

import com.ordersession.auth.OrderSessionAuth;
import com.ordersession.config.ConfigManager;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * After {@link com.ordersession.tests.BaseTestTemplate} {@code @BeforeSuite} prefetches the token
 * (OAuth client-credentials or JWT), this test asserts {@link OrderSessionAuth#bearerToken()} is usable
 * for all authenticated Order Session calls.
 */
@Epic("Order Session API")
@Feature("Auth bootstrap (OAuth / JWT)")
public class ExternalApiAuthBootstrapTests {

    @Test(priority = -100, groups = {"Regression", "Sanity", "Smoke"})
    @Story("Resolve bearer via CLIENT / SECURE / GLOFOX token endpoints from merged OAuth file or JWT")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Asserts suite-prefetched OrderSessionAccessToken (same token reused on every authorized API call).")
    public void warmBearerFromOAuthOrJwt() {
        if (!OrderSessionAuth.isAuthConfigured()) {
            throw new SkipException(
                    "Create config/oauth-env.local.properties (see config/oauth-env.local.properties.example), or set ORDER_SESSION_JWT, or auth.bearer.token");
        }

        Allure.step("order.session.auth.source=" + ConfigManager.getOptional("order.session.auth.source"));
        Allure.step("external merge loaded=" + ConfigManager.hasExternalMerge());
        Allure.step("Bearer from suite prefetch / token endpoint (cached OrderSessionAccessToken)");

        String token = OrderSessionAuth.bearerToken();
        Assert.assertFalse(OrderSessionAuth.isPlaceholderToken(token), "Bearer token must not be a placeholder");
        Allure.step("Bearer ready (masked): " + token.substring(0, Math.min(12, token.length())) + "…");
    }
}
