package com.ordersession.support;

import com.ordersession.auth.OrderSessionAuth;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OrderSessionHeaders {

    private OrderSessionHeaders() {
    }

    public static Map<String, String> jsonAuthenticated() {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Authorization", "Bearer " + OrderSessionAuth.bearerToken());
        h.put("Content-Type", "application/json;charset=UTF-8");
        h.put("ABCFS-ORGANIZATION-ID", OrderSessionTestConfig.organizationId());
        return h;
    }

    public static Map<String, String> jsonNoAuth() {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Content-Type", "application/json;charset=UTF-8");
        h.put("ABCFS-ORGANIZATION-ID", OrderSessionTestConfig.organizationId());
        return h;
    }

    public static Map<String, String> getAuthenticated() {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Authorization", "Bearer " + OrderSessionAuth.bearerToken());
        h.put("ABCFS-ORGANIZATION-ID", OrderSessionTestConfig.organizationId());
        return h;
    }
}
