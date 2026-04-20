package com.billing.tests.support;

import io.restassured.response.Response;
import org.testng.Assert;

public final class ErrorResponseAssertions {

    private ErrorResponseAssertions() {
    }

    public static void assertErrorEnvelope(Response response) {
        String body = response.asString();
        Assert.assertTrue(body.contains("statusCode") || body.contains("errors"),
                "Expected ErrorResponse shape, got: " + body);
    }
}
