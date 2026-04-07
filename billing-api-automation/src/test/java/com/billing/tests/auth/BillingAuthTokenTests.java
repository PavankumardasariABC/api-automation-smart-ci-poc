package com.billing.tests.auth;

import com.billing.auth.BillingAuth;
import com.billing.config.ConfigManager;
import com.billing.store.ResponseStore;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import com.billing.tests.support.Preconditions;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * TDD: Authorization API must return access_token; token is cached for reuse across Billing calls.
 */
@Epic("Billing API")
@Feature("Commerce Authorization")
@Severity(SeverityLevel.BLOCKER)
public class BillingAuthTokenTests {

    @BeforeClass(alwaysRun = true)
    public void requireAuthPrereq() {
        Preconditions.skipUnless(BillingAuth.canResolveBearer(),
                "Set BILLING_OAUTH_PASSWORD or billing.oauth.password, or JWT via BILLING_COMMERCE_JWT / -Dauth.bearer.token");
    }

    @Story("Client credentials return bearer token")
    @Test(groups = {"Regression", "Smoke", "Sanity"}, priority = 1,
            description = "GET equivalent: OAuth token endpoint returns 200/201 and access_token; cached under BillingAccessToken")
    public void clientCredentials_returnsAccessToken_andCaches() {
        ResponseStore.remove(BillingAuth.CACHE_KEY);
        String first = BillingAuth.bearerToken();
        Assert.assertNotNull(first);
        Assert.assertFalse(first.isBlank());

        String second = BillingAuth.bearerToken();
        Assert.assertEquals(second, first, "Second call should reuse cached bearer");

        Object cached = ResponseStore.get(BillingAuth.CACHE_KEY);
        Assert.assertEquals(cached, first);
    }

    @Story("Environment-specific auth URL")
    @Test(groups = {"Regression", "Smoke"}, priority = 2)
    public void authUrl_matchesSelectedEnv() {
        String url = ConfigManager.get("billing.auth.url");
        String env = ConfigManager.getEnv();
        Assert.assertTrue(url.contains("commerce.abc.fitness"), url);
        Assert.assertTrue(url.contains("/api/authorization/token"), url);
        Assert.assertTrue(url.contains("api-" + env + "."),
                "Auth URL should target env [" + env + "]: " + url);
    }
}
