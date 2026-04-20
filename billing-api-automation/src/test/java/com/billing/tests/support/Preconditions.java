package com.billing.tests.support;

import org.testng.SkipException;

/**
 * TestNG-friendly assumptions (skip vs fail) — TestNG has no {@code org.testng.Assume}.
 */
public final class Preconditions {

    private Preconditions() {
    }

    public static void skipUnless(boolean condition, String message) {
        if (!condition) {
            throw new SkipException(message);
        }
    }

    public static void skipIf(boolean condition, String message) {
        if (condition) {
            throw new SkipException(message);
        }
    }
}
