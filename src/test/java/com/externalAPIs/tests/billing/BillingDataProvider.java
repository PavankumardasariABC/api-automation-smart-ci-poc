package com.externalAPIs.tests.billing;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.testng.annotations.DataProvider;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Data providers for Billing APIs.
 * Template: Copy this class and adjust file path / keys for new billing-related APIs.
 */
public final class BillingDataProvider {

    private static final String TRANSFER_DATA_PATH = "src/test/resources/input/billing/create_billing_account_transfer_data.json";

    /**
     * Provides scenarios for Billing Account Transfer API.
     * Uses {@link BillingTransferResponseStore#resolveTargetLocationId} (transfer / location flows) then default UUID.
     */
    @DataProvider(name = "billingAccountTransferData")
    public static Iterator<Object[]> getBillingAccountTransferData() throws Exception {
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> data = new Gson().fromJson(new FileReader(TRANSFER_DATA_PATH), listType);

        String defaultLocation = "11ec0af2-3a19-b7d3-a84f-59243ef7e239";
        String locationIdFromStore = BillingTransferResponseStore.resolveTargetLocationId(defaultLocation);

        List<Object[]> rows = new ArrayList<>();
        for (Map<String, Object> row : data) {
            Map<String, Object> copy = new HashMap<>(row);
            copy.put("locationId", locationIdFromStore);
            rows.add(new Object[]{ copy });
        }
        return rows.iterator();
    }
}
