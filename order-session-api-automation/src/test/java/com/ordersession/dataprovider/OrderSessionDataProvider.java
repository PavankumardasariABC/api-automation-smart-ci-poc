package com.ordersession.dataprovider;

import com.ordersession.config.ConfigManager;
import org.testng.annotations.DataProvider;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Hybrid data: owner types and optional overrides from configuration.
 */
public final class OrderSessionDataProvider {

    private OrderSessionDataProvider() {
    }

    @DataProvider(name = "paymentTokenOwnerTypes")
    public static Iterator<Object[]> paymentTokenOwnerTypes() {
        List<String> types = List.of("PAYOR", "LOCATION", "MEMBER");
        return types.stream().map(t -> new Object[]{t}).iterator();
    }

    @DataProvider(name = "locationAndMemberOwnerTypes")
    public static Iterator<Object[]> locationAndMemberOwnerTypes() {
        return Stream.of("LOCATION", "MEMBER").map(t -> new Object[]{t}).iterator();
    }

    @DataProvider(name = "walletEntryInvalidBodies")
    public static Iterator<Object[]> walletInvalidBodies() {
        return Stream.of(
                new Object[]{"missing ownerId", Map.of(
                        "consumerId", sampleConsumerId(),
                        "ownerType", "PAYOR",
                        "locationId", sampleLocationId(),
                        "user", Map.of(
                                "id", sampleUserId(),
                                "externalId", sampleUserExternalId()
                        )
                )},
                new Object[]{"missing user", Map.of(
                        "consumerId", sampleConsumerId(),
                        "ownerType", "PAYOR",
                        "ownerId", sampleOwnerId(),
                        "locationId", sampleLocationId()
                )}
        ).iterator();
    }

    public static String sampleConsumerId() {
        return ConfigManager.getOptional("test.consumer.id") != null
                ? ConfigManager.getOptional("test.consumer.id")
                : "00000000-0000-4000-8000-000000000001";
    }

    public static String sampleLocationId() {
        return ConfigManager.getOptional("test.location.id") != null
                ? ConfigManager.getOptional("test.location.id")
                : "00000000-0000-4000-8000-000000000002";
    }

    public static String sampleOwnerId() {
        return ConfigManager.getOptional("test.owner.id") != null
                ? ConfigManager.getOptional("test.owner.id")
                : "00000000-0000-4000-8000-000000000003";
    }

    public static String sampleUserId() {
        return ConfigManager.getOptional("test.wallet.user.id") != null
                ? ConfigManager.getOptional("test.wallet.user.id")
                : "00000000-0000-4000-8000-000000000004";
    }

    public static String sampleUserExternalId() {
        return ConfigManager.getOptional("test.wallet.user.external.id") != null
                ? ConfigManager.getOptional("test.wallet.user.external.id")
                : "00000000-0000-4000-8000-000000000005";
    }

    public static Map<String, Object> validPaymentTokenBody(String ownerType) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("consumerId", sampleConsumerId());
        m.put("ownerType", ownerType);
        m.put("locationId", sampleLocationId());
        return m;
    }

    public static Map<String, Object> validWalletEntryBody() {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", sampleUserId());
        user.put("externalId", sampleUserExternalId());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("consumerId", sampleConsumerId());
        m.put("ownerType", "PAYOR");
        m.put("ownerId", sampleOwnerId());
        m.put("locationId", sampleLocationId());
        m.put("user", user);
        m.put("consumerOrigin", ConfigManager.getOptional("test.consumer.origin"));
        m.put("paymentMethods", List.of("BANK", "CARD"));
        m.put("supportedTags", List.of("PRIMARY", "ALTERNATE"));
        if (m.get("consumerOrigin") == null || ((String) m.get("consumerOrigin")).isBlank()) {
            m.put("consumerOrigin", "https://dc01dapigateway.abcfinancial.net:8080");
        }
        return m;
    }
}
