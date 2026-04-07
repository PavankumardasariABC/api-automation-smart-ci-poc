package com.ordersession.dataprovider;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.testng.annotations.DataProvider;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JSON-driven scenarios (same pattern as {@code BillingDataProvider} in the root POC).
 */
public final class OrderSessionApiTemplateDataProvider {

    private static final String PAYMENT_TOKEN_DATA =
            "src/test/resources/input/order-session/create_payment_token_session_data.json";
    private static final String WALLET_ENTRY_DATA =
            "src/test/resources/input/order-session/create_wallet_entry_session_data.json";

    private OrderSessionApiTemplateDataProvider() {
    }

    @DataProvider(name = "orderSessionPaymentTokenCreateData")
    public static Iterator<Object[]> paymentTokenCreateData() throws Exception {
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> rows = new Gson().fromJson(new FileReader(PAYMENT_TOKEN_DATA), listType);
        List<Object[]> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            out.add(new Object[]{new HashMap<>(row)});
        }
        return out.iterator();
    }

    @DataProvider(name = "orderSessionWalletEntryCreateData")
    public static Iterator<Object[]> walletEntryCreateData() throws Exception {
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> rows = new Gson().fromJson(new FileReader(WALLET_ENTRY_DATA), listType);
        List<Object[]> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            out.add(new Object[]{new HashMap<>(row)});
        }
        return out.iterator();
    }
}
