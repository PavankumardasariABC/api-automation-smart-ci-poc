package com.ordersession.tests;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

/**
 * Non-HTTP narrative test: documents the end-to-end product flow from the OpenAPI overview for demos
 * (Allure “story” steps). Keeps executable E2E in *IntegrationTests classes.
 */
@Epic("Order Session API")
@Feature("E2E story (documentation)")
public class OrderSessionE2eHighLevelTest {

    @Test(groups = {"Regression"})
    @Description("High-level flow from openapi-spec-5: hosted paypage and wallet entry; automation covers API up to CREATED/poll.")
    public void openapiDocumented_endToEnd_flow() {
        Allure.step("Payment token: POST /payment-session/tokens → receive session id + hosted url");
        Allure.step("Consumer completes paypage (out of scope for API automation)");
        Allure.step("GET /payment-session/tokens/{id} → status CREATED until complete, then CLOSED with token when applicable");
        Allure.step("Wallet entry: POST /payment-session/wallet-entries → hosted url; GET omits url per spec");
        Allure.step("This suite validates contracts, auth, validation, create + GET while CREATED/CLOSED as environment allows");
    }
}
