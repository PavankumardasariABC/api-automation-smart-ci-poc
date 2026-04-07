package com.ordersession.support;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;

public final class ErrorPayloadAssertions {

    private ErrorPayloadAssertions() {
    }

    /**
     * Asserts OpenAPI {@code ErrorPayload}: statusCode string + errors[] with code, message, messageKey.
     */
    public static void assertErrorPayloadShape(Response res) {
        JsonPath jp = res.jsonPath();
        String statusCode = jp.getString("statusCode");
        Assert.assertNotNull(statusCode, "statusCode missing in error body");
        Object errors = jp.get("errors");
        Assert.assertNotNull(errors, "errors missing");
        int n = jp.getList("errors").size();
        Assert.assertTrue(n >= 1, "errors should have at least one item");
        Assert.assertNotNull(jp.getString("errors[0].code"), "errors[0].code");
        Assert.assertNotNull(jp.getString("errors[0].message"), "errors[0].message");
        Assert.assertNotNull(jp.getString("errors[0].messageKey"), "errors[0].messageKey");
    }
}
