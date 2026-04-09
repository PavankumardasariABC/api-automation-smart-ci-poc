package com.ordersession.support;

import io.restassured.path.json.JsonPath;
import org.testng.Assert;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * OpenAPI-aligned assertions for successful (2xx) Order Session responses — positive contract checks.
 */
public final class OrderSessionResponseContractAssertions {

    private OrderSessionResponseContractAssertions() {
    }

    /**
     * {@code POST /payment-session/tokens} → {@code PaymentSession} (201).
     */
    public static void assertPaymentSessionPost201(
            JsonPath jp, String expectedConsumerId, String expectedOwnerType, String expectedLocationId) {
        Assert.assertNotNull(jp.getString("id"), "id");
        Assert.assertEquals(jp.getString("consumerId"), expectedConsumerId, "consumerId");
        Assert.assertEquals(jp.getString("ownerType"), expectedOwnerType, "ownerType");
        Assert.assertEquals(jp.getString("locationId"), expectedLocationId, "locationId");
        Assert.assertEquals(jp.getString("status"), "CREATED", "status");
        Assert.assertNotNull(jp.getString("created"), "created");
        Assert.assertNotNull(jp.getString("modified"), "modified");
        Assert.assertNotNull(jp.getString("url"), "url");
        String expirationType = jp.getString("expirationType");
        Assert.assertTrue(
                "DYNAMIC".equals(expirationType) || "STATIC".equals(expirationType),
                "expirationType must be DYNAMIC or STATIC, got: " + expirationType);
    }

    /**
     * {@code GET /payment-session/tokens/{id}} when {@code status=CREATED} — token absent or null.
     */
    public static void assertPaymentSessionTokenGetCreatedState(JsonPath jp, String expectedSessionId) {
        Assert.assertEquals(jp.getString("id"), expectedSessionId, "id");
        Assert.assertEquals(jp.getString("status"), "CREATED", "status");
        Assert.assertNotNull(jp.getString("consumerId"), "consumerId");
        Assert.assertNotNull(jp.getString("ownerType"), "ownerType");
        Assert.assertNotNull(jp.getString("locationId"), "locationId");
        String expirationType = jp.getString("expirationType");
        Assert.assertTrue(
                "DYNAMIC".equals(expirationType) || "STATIC".equals(expirationType),
                "expirationType");
        Assert.assertNull(jp.get("token"), "token should be null/absent when status is CREATED");
    }

    /**
     * {@code GET /payment-session/tokens/{id}} when {@code status=CLOSED} — {@code PaymentSessionToken.token} populated.
     */
    public static void assertPaymentSessionTokenGetClosedState(JsonPath jp) {
        Assert.assertEquals(jp.getString("status"), "CLOSED", "status");
        Assert.assertNotNull(jp.getString("token.id"), "token.id");
        String tokenType = jp.getString("token.type");
        Assert.assertTrue(
                "CREDIT_CARD".equals(tokenType) || "BANK_ACCOUNT".equals(tokenType),
                "token.type: " + tokenType);
        String tokenStatus = jp.getString("token.status");
        Assert.assertNotNull(tokenStatus, "token.status");
        Object usePm = jp.get("token.useAsPaymentMethod");
        Assert.assertTrue(usePm instanceof Boolean, "token.useAsPaymentMethod");
        Assert.assertNotNull(jp.getString("token.created"), "token.created");
        Assert.assertNotNull(jp.getString("token.modified"), "token.modified");
        if ("CREDIT_CARD".equals(tokenType)) {
            Assert.assertNotNull(jp.getString("token.creditCard.cardNumberLastFour"), "creditCard.cardNumberLastFour");
            Assert.assertNotNull(jp.getString("token.creditCard.cardBrand"), "creditCard.cardBrand");
        } else {
            Assert.assertNotNull(jp.getString("token.bankAccount.accountNumberLastFour"), "bankAccount.accountNumberLastFour");
            Assert.assertNotNull(jp.getString("token.bankAccount.routingNumber"), "bankAccount.routingNumber");
        }
    }

    /**
     * {@code GET /payment-session/tokens/{id}} when {@code status=EXPIRED}.
     */
    public static void assertPaymentSessionTokenGetExpiredState(JsonPath jp) {
        Assert.assertEquals(jp.getString("status"), "EXPIRED", "status");
    }

    /**
     * {@code POST /payment-session/wallet-entries} → {@code WalletEntrySession} (201).
     */
    @SuppressWarnings("unchecked")
    public static void assertWalletEntrySessionPost201(JsonPath jp, Map<String, Object> requestBody) {
        Assert.assertNotNull(jp.getString("id"), "id");
        Assert.assertEquals(jp.getString("consumerId"), Objects.toString(requestBody.get("consumerId")), "consumerId");
        Assert.assertEquals(jp.getString("ownerType"), "PAYOR", "ownerType");
        Assert.assertEquals(jp.getString("status"), "CREATED", "status");
        Assert.assertNotNull(jp.getString("created"), "created");
        Assert.assertNotNull(jp.getString("modified"), "modified");
        Assert.assertNotNull(jp.getString("url"), "url");
        String expirationType = jp.getString("expirationType");
        Assert.assertTrue(
                "DYNAMIC".equals(expirationType) || "STATIC".equals(expirationType),
                "expirationType");
        // Backend may resolve/normalize ownerId (not always a byte-for-byte echo of the request).
        Assert.assertNotNull(jp.getString("ownerId"), "ownerId");
        Assert.assertEquals(jp.getString("locationId"), Objects.toString(requestBody.get("locationId")), "locationId");
        Assert.assertEquals(jp.getString("user.id"), Objects.toString(((Map<?, ?>) requestBody.get("user")).get("id")), "user.id");
        Assert.assertEquals(
                jp.getString("user.externalId"),
                Objects.toString(((Map<?, ?>) requestBody.get("user")).get("externalId")),
                "user.externalId");

        if (requestBody.containsKey("consumerOrigin") && requestBody.get("consumerOrigin") != null
                && !Objects.toString(requestBody.get("consumerOrigin")).isBlank()) {
            Assert.assertEquals(
                    jp.getString("consumerOrigin"),
                    Objects.toString(requestBody.get("consumerOrigin")),
                    "consumerOrigin echo");
        }
        if (requestBody.containsKey("paymentMethods") && requestBody.get("paymentMethods") != null) {
            List<?> expected = (List<?>) requestBody.get("paymentMethods");
            List<?> actual = jp.getList("paymentMethods");
            Assert.assertEquals(actual, expected, "paymentMethods echo");
        }
        if (requestBody.containsKey("supportedTags") && requestBody.get("supportedTags") != null) {
            List<?> expected = (List<?>) requestBody.get("supportedTags");
            List<?> actual = jp.getList("supportedTags");
            Assert.assertEquals(actual, expected, "supportedTags echo");
        }
        if (requestBody.containsKey("metadata") && requestBody.get("metadata") != null) {
            Map<?, ?> expected = (Map<?, ?>) requestBody.get("metadata");
            Map<?, ?> actual = jp.getMap("metadata");
            Assert.assertEquals(actual, expected, "metadata echo");
        }
    }

    /**
     * {@code GET /payment-session/wallet-entries/{id}} when {@code status=CREATED} — no hosted {@code url}.
     */
    public static void assertWalletEntrySessionGetCreatedState(JsonPath jp, String expectedSessionId) {
        Assert.assertEquals(jp.getString("id"), expectedSessionId, "id");
        Assert.assertEquals(jp.getString("status"), "CREATED", "status");
        Object url = jp.get("url");
        Assert.assertTrue(url == null || url.toString().isEmpty(), "url must not be present on GET");
        Assert.assertNull(jp.get("token"), "token should be null/absent when status is CREATED");
    }

    /**
     * {@code GET /payment-session/wallet-entries/{id}} when {@code status=CLOSED} — token populated.
     */
    public static void assertWalletEntrySessionGetClosedState(JsonPath jp) {
        Assert.assertEquals(jp.getString("status"), "CLOSED", "status");
        Assert.assertNotNull(jp.getString("token.id"), "token.id");
        String tokenType = jp.getString("token.type");
        Assert.assertTrue(
                "CREDIT_CARD".equals(tokenType) || "BANK_ACCOUNT".equals(tokenType),
                "token.type: " + tokenType);
        Object url = jp.get("url");
        Assert.assertTrue(url == null || url.toString().isEmpty(), "url must not be on GET");
        if ("CREDIT_CARD".equals(tokenType)) {
            Assert.assertNotNull(jp.getString("token.creditCard.cardBrand"), "creditCard.cardBrand");
        } else {
            Assert.assertNotNull(jp.getString("token.bankAccount.routingNumber"), "bankAccount.routingNumber");
        }
    }

    /**
     * {@code GET /payment-session/wallet-entries/{id}} when {@code status=EXPIRED}.
     */
    public static void assertWalletEntrySessionGetExpiredState(JsonPath jp) {
        Assert.assertEquals(jp.getString("status"), "EXPIRED", "status");
    }
}
