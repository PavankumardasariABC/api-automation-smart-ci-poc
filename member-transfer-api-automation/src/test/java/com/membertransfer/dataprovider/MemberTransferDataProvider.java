package com.membertransfer.dataprovider;

import com.membertransfer.support.MemberTransferSupport;
import com.google.gson.GsonBuilder;
import org.testng.annotations.DataProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-driven scenarios for bulk inquiry.
 */
public final class MemberTransferDataProvider {

    private static final com.google.gson.Gson G = new GsonBuilder().disableHtmlEscaping().create();

    private MemberTransferDataProvider() {
    }

    @DataProvider(name = "memberTransferInquiryNegativeScenarios")
    public static Object[][] inquiryNegatives() {
        String validFrom = memberFrom();
        String validTo = memberTo();
        return new Object[][] {
                { "empty items", G.toJson(Map.of("userName", "u", "items", List.of())), 400, 499 },
                { "items null", "{\"userName\":\"u\",\"items\":null}", 400, 499 },
                { "bad from UUID", buildOneItem("not-uuid", validTo), 400, 499 },
                { "bad to UUID", buildOneItem(validFrom, "bad-loc"), 400, 499 }
        };
    }

    @DataProvider(name = "memberTransferInquiryEdgeScenarios")
    public static Object[][] inquiryEdges() {
        String u = memberFrom();
        String v = memberTo();
        Map<String, Object> br = new LinkedHashMap<>(MemberTransferSupport.defaultBrandRules());
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("fromBillingAccountId", u);
        item.put("toLocationId", v);
        item.put("brandRulesPreference", br);
        return new Object[][] {
                { "omit userName", G.toJson(Map.of("items", List.of(item))) },
                { "minimal item (no brand rules)", G.toJson(Map.of("userName", "edge_user", "items", List.of(
                        Map.of("fromBillingAccountId", u, "toLocationId", v)
                ))) }
        };
    }

    private static String buildOneItem(String from, String to) {
        Map<String, Object> it = new LinkedHashMap<>();
        it.put("fromBillingAccountId", from);
        it.put("toLocationId", to);
        it.put("brandRulesPreference", MemberTransferSupport.defaultBrandRules());
        return G.toJson(Map.of("userName", "doc_user", "items", List.of(it)));
    }

    private static String memberFrom() {
        try {
            return MemberTransferSupport.fromBillingAccountId();
        } catch (Exception e) {
            return "11ec0af2-3505-d260-a84f-59243ef7e239";
        }
    }

    private static String memberTo() {
        try {
            return MemberTransferSupport.toLocationId();
        } catch (Exception e) {
            return "11ec0af2-3a19-b7d3-a84f-59243ef7e239";
        }
    }
}
