package com.billing.tests.support;

import com.billing.config.ConfigManager;

public final class BillingTestConditions {

    private BillingTestConditions() {
    }

    public static void assumeOrgConfigured() {
        String org = ConfigManager.getOptional("billing.organization.id");
        Preconditions.skipUnless(org != null && !org.isBlank() && !org.startsWith("REPLACE_"),
                "billing.organization.id must be set in env/*.local.properties (not REPLACE_*)");
    }

    public static String optionalBillingAccountId() {
        return ConfigManager.getOptional("test.billing.account.id");
    }

    public static void assumeBillingAccountForNested() {
        assumeOrgConfigured();
        String id = optionalBillingAccountId();
        Preconditions.skipUnless(id != null && !id.isBlank(), "test.billing.account.id for nested tests");
    }

    public static boolean hasLocationHeader() {
        String loc = ConfigManager.getOptional("billing.location.id");
        return loc != null && !loc.isBlank();
    }
}
