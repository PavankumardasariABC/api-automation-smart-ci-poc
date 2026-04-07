package com.billing.service;

import com.billing.auth.BillingAuth;
import com.billing.config.ConfigManager;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BillingHeaders {

    private BillingHeaders() {
    }

    public static Map<String, String> bearerJson(String token) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Authorization", "Bearer " + token);
        h.put("Accept", "application/json;charset=UTF-8");
        h.put("Content-Type", "application/json;charset=UTF-8");
        h.put("ABCFS-ORGANIZATION-ID", ConfigManager.get("billing.organization.id"));
        h.put("User-Agent", "Billing-Automation/1.0");
        return h;
    }

    public static Map<String, String> bearerJsonWithLocation(String token) {
        Map<String, String> h = bearerJson(token);
        String loc = ConfigManager.getOptional("billing.location.id");
        if (loc != null && !loc.isBlank()) {
            h.put("ABCFS-LOCATION-ID", loc.trim());
        }
        return h;
    }

    public static Map<String, String> defaultAuthorized() {
        return bearerJson(BillingAuth.bearerToken());
    }

    public static Map<String, String> missingAuthButOrg() {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Accept", "application/json;charset=UTF-8");
        h.put("ABCFS-ORGANIZATION-ID", ConfigManager.get("billing.organization.id"));
        return h;
    }

    public static Map<String, String> invalidBearer() {
        Map<String, String> h = bearerJson("invalid.token.value");
        return h;
    }
}
