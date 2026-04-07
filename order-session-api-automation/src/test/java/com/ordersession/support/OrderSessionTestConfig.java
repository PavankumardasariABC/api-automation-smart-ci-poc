package com.ordersession.support;

import com.ordersession.auth.OrderSessionAuth;
import com.ordersession.config.ConfigManager;

/**
 * Gates integration tests and resolves values that must come from <strong>config only</strong>
 * ({@code env/*.properties} / {@code *.local.properties}) — not from Java or JSON data files.
 */
public final class OrderSessionTestConfig {

    /** Spec header name; values come from {@code abcfs.organization.id} or alias {@code organization.id}. */
    public static final String CONFIG_ORGANIZATION_PRIMARY = "abcfs.organization.id";
    public static final String CONFIG_ORGANIZATION_ALIAS = "organization.id";

    private OrderSessionTestConfig() {
    }

    /**
     * Tenant UUID for {@code ABCFS-ORGANIZATION-ID}. Resolution order:
     * {@code abcfs.organization.id} → {@code organization.id} (first non-blank across local / module / external merge).
     */
    public static String organizationId() {
        String id = resolveOrganizationIdRaw();
        if (id == null) {
            throw new IllegalStateException(
                    "Set " + CONFIG_ORGANIZATION_PRIMARY + " or " + CONFIG_ORGANIZATION_ALIAS
                            + " in src/test/resources/env/<env>.properties or <env>.local.properties");
        }
        return id;
    }

    /**
     * Raw org id from config. If {@code abcfs.organization.id} is still a template placeholder, the
     * value from {@code organization.id} is used when set (typical: committed placeholder + real UUID in *.local.properties).
     */
    public static String resolveOrganizationIdRaw() {
        String primary = firstNonBlank(ConfigManager.getOptional(CONFIG_ORGANIZATION_PRIMARY));
        String alias = firstNonBlank(ConfigManager.getOptional(CONFIG_ORGANIZATION_ALIAS));
        if (!isTemplateOrganizationValue(primary)) {
            return primary;
        }
        if (alias != null) {
            return alias;
        }
        return primary;
    }

    private static boolean isTemplateOrganizationValue(String id) {
        if (id == null || id.isBlank()) {
            return true;
        }
        String u = id.toUpperCase();
        return u.contains("REPLACE") || u.contains("YOUR_") || u.contains("TODO");
    }

    public static boolean hasValidToken() {
        try {
            String t = OrderSessionAuth.bearerToken();
            return !OrderSessionAuth.isPlaceholderToken(t);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isPaymentTokenFlowReady() {
        if (!hasValidToken()) {
            return false;
        }
        return nonBlank(ConfigManager.getOptional("test.consumer.id"))
                && nonBlank(ConfigManager.getOptional("test.location.id"));
    }

    public static boolean isWalletEntryFlowReady() {
        if (!isPaymentTokenFlowReady()) {
            return false;
        }
        return nonBlank(ConfigManager.getOptional("test.owner.id"))
                && nonBlank(ConfigManager.getOptional("test.wallet.user.id"))
                && nonBlank(ConfigManager.getOptional("test.wallet.user.external.id"));
    }

    /**
     * True when organization is a real UUID, not a template placeholder.
     */
    public static boolean hasRealOrganizationId() {
        try {
            String id = resolveOrganizationIdRaw();
            if (id == null || id.isBlank() || isTemplateOrganizationValue(id)) {
                return false;
            }
            return id.trim().matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        } catch (Exception e) {
            return false;
        }
    }

    private static String firstNonBlank(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean nonBlank(String s) {
        return s != null && !s.isBlank();
    }
}
