package com.billing.config;

import com.billing.store.ResponseStore;
import io.qameta.allure.Allure;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.restassured.RestAssured.given;

public final class ApiClient {

    private static final int MAX_RETRIES = 3;
    private static final AllureRestAssured ALLURE_FILTER = new AllureRestAssured();

    public static Response get(String url, Map<String, String> headers) {
        return executeWithRetry("GET", url, headers, null, null);
    }

    public static Response post(String url, Map<String, String> headers, Object body) {
        return executeWithRetry("POST", url, headers, body, null);
    }

    public static Response post(String url, Map<String, String> headers, Object body, Map<String, String> queryParams) {
        return executeWithRetry("POST", url, headers, body, queryParams);
    }

    public static Response patch(String url, Map<String, String> headers, Object body) {
        return executeWithRetry("PATCH", url, headers, body, null);
    }

    public static Response delete(String url, Map<String, String> headers) {
        return executeWithRetry("DELETE", url, headers, null, null);
    }

    private static Response executeWithRetry(String method, String url, Map<String, String> headers,
                                             Object body, Map<String, String> queryParams) {
        Response response = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                RequestSpecification request = given()
                        .relaxedHTTPSValidation()
                        .headers(headers)
                        .filter(ALLURE_FILTER)
                        .log().all();

                if (queryParams != null && !queryParams.isEmpty()) {
                    request.queryParams(queryParams);
                }

                if (body != null) {
                    String contentType = headers.getOrDefault("Content-Type", "").toLowerCase();
                    if (contentType.contains("x-www-form-urlencoded") && body instanceof Map<?, ?> formData) {
                        request.contentType(ContentType.URLENC);
                        @SuppressWarnings("unchecked")
                        Map<String, ?> form = (Map<String, ?>) formData;
                        request.formParams(form);
                    } else if (contentType.contains("merge-patch+json")) {
                        request.contentType("application/merge-patch+json");
                        request.body(body);
                    } else {
                        request.contentType(ContentType.JSON);
                        request.body(body);
                    }
                }

                Allure.step("➡️ " + method + " " + url);

                response = switch (method.toUpperCase()) {
                    case "POST" -> request.when().post(url).then().log().all().extract().response();
                    case "GET" -> request.when().get(url).then().log().all().extract().response();
                    case "PATCH" -> request.when().patch(url).then().log().all().extract().response();
                    case "DELETE" -> request.when().delete(url).then().log().all().extract().response();
                    default -> throw new IllegalArgumentException("Unsupported method: " + method);
                };

                logRequestAndResponse(method, url, headers, body, response, attempt);

                if (response.statusCode() < 500) {
                    break;
                }
                Allure.step("⚠️ Retryable " + response.statusCode() + " attempt " + attempt);
            } catch (Exception e) {
                Allure.step("❌ Exception attempt " + attempt + ": " + e.getMessage());
                if (attempt == MAX_RETRIES) {
                    throw new RuntimeException("API call failed after retries: " + e.getMessage(), e);
                }
            }
        }

        if (response == null) {
            throw new RuntimeException("No response for " + method + " " + url);
        }
        return response;
    }

    private static void logRequestAndResponse(String method, String url, Map<String, String> headers,
                                              Object body, Response response, int attempt) {
        if (body != null) {
            ResponseStore.put(method + "_RequestBody", body);
        }
        ResponseStore.put(method + "_ResponseBody", response.asPrettyString());

        try {
            Allure.addAttachment("📤 " + method + " (attempt " + attempt + ")",
                    new ByteArrayInputStream(
                            (url + "\nHeaders:\n" + maskSensitive(headers) + "\n\nBody:\n" + body)
                                    .getBytes(StandardCharsets.UTF_8)));
            Allure.addAttachment("📥 Response (" + response.statusCode() + ")",
                    new ByteArrayInputStream(response.asPrettyString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            System.err.println("Allure attach failed: " + e.getMessage());
        }
    }

    private static String maskSensitive(Map<String, String> headers) {
        StringBuilder sb = new StringBuilder("{ ");
        headers.forEach((k, v) -> {
            if (k.toLowerCase().contains("authorization")) {
                sb.append(k).append("=****, ");
            } else {
                sb.append(k).append("=").append(v).append(", ");
            }
        });
        return sb.append("}").toString();
    }
}
