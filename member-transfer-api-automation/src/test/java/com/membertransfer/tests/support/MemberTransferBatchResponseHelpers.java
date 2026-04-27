package com.membertransfer.tests.support;

import io.restassured.path.json.JsonPath;

import java.util.List;
import java.util.Optional;

/**
 * Tolerates Spring Data {@code Page} (root {@code content}) and {@code itemResults} wrappers.
 */
public final class MemberTransferBatchResponseHelpers {

    private MemberTransferBatchResponseHelpers() {
    }

    public static List<?> contentList(JsonPath jp) {
        List<?> c = jp.getList("content");
        if (c == null) {
            c = jp.getList("itemResults.content");
        }
        return c;
    }

    public static Optional<Integer> totalElements(JsonPath jp) {
        try {
            Object o = jp.get("totalElements");
            if (o == null) {
                o = jp.get("itemResults.totalElements");
            }
            if (o == null) {
                return Optional.empty();
            }
            if (o instanceof Number n) {
                return Optional.of(n.intValue());
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    public static Optional<Integer> pageNumber(JsonPath jp) {
        try {
            Object o = jp.get("number");
            if (o == null) {
                o = jp.get("itemResults.number");
            }
            if (o == null) {
                return Optional.empty();
            }
            if (o instanceof Number n) {
                return Optional.of(n.intValue());
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    public static Optional<Integer> pageSizeField(JsonPath jp) {
        try {
            Object o = jp.get("size");
            if (o == null) {
                o = jp.get("itemResults.size");
            }
            if (o == null) {
                return Optional.empty();
            }
            if (o instanceof Number n) {
                return Optional.of(n.intValue());
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }
}
