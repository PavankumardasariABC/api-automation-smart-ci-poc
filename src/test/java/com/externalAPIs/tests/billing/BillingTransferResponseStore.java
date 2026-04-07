package com.externalAPIs.tests.billing;

import com.externalAPIs.store.ResponseStore;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.Optional;

/**
 * Template helper: persist Billing Account Transfer API success responses and resolve values for chained tests.
 * <p>
 * Keys are shared across {@link CreateBillingAccountTransferTests}, {@link BillingDataProvider}, and other
 * billing suites that run later in the suite.
 * </p>
 */
public final class BillingTransferResponseStore {

    public static final String KEY_TRANSFER_ID = "billingAccountTransferId";
    public static final String KEY_TARGET_LOCATION_ID = "billingTransferTargetLocationId";
    public static final String KEY_REASON_CODE_ID = "billingAccountTransferReasonCodeId";
    public static final String KEY_LAST_STATUS = "billingAccountTransferStatus";
    public static final String KEY_LAST_RESPONSE_JSON = "billingTransferLastResponseJson";

    /** Also mirrors target location as {@code locationId} for {@link BillingDataProvider} / CreateLocation flows. */
    public static final String LEGACY_LOCATION_KEY = "locationId";

    private BillingTransferResponseStore() {
    }

    /**
     * After a 2xx transfer or dry-run response, extract id, locationId, status, reasonCodeId and persist.
     */
    public static void persistFromSuccessResponse(Response response) {
        int code = response.statusCode();
        if (code < 200 || code >= 300) {
            return;
        }
        JsonPath jp = response.jsonPath();
        String id = jp.getString("id");
        if (id != null && !id.isBlank()) {
            ResponseStore.put(KEY_TRANSFER_ID, id.trim());
        }
        String locationId = jp.getString("locationId");
        if (locationId != null && !locationId.isBlank()) {
            String loc = locationId.trim();
            ResponseStore.put(KEY_TARGET_LOCATION_ID, loc);
            ResponseStore.put(LEGACY_LOCATION_KEY, loc);
        }
        String status = jp.getString("status");
        if (status != null && !status.isBlank()) {
            ResponseStore.put(KEY_LAST_STATUS, status.trim());
        }
        String reasonCodeId = jp.getString("reasonCodeId");
        if (reasonCodeId != null && !reasonCodeId.isBlank()) {
            ResponseStore.put(KEY_REASON_CODE_ID, reasonCodeId.trim());
        }
        ResponseStore.put(KEY_LAST_RESPONSE_JSON, response.asPrettyString());
    }

    /**
     * Target location for POST bodies: last successful transfer response, then legacy locationId, then default.
     */
    public static String resolveTargetLocationId(String fallbackUuid) {
        return firstNonBlank(
                ResponseStore.get(KEY_TARGET_LOCATION_ID),
                ResponseStore.get(LEGACY_LOCATION_KEY))
                .orElse(fallbackUuid);
    }

    public static Optional<String> transferId() {
        return firstNonBlank(ResponseStore.get(KEY_TRANSFER_ID));
    }

    private static Optional<String> firstNonBlank(Object... candidates) {
        if (candidates == null) {
            return Optional.empty();
        }
        for (Object o : candidates) {
            if (o == null) {
                continue;
            }
            String s = o.toString().trim();
            if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
}
