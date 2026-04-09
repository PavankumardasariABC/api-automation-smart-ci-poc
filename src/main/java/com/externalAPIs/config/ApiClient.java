package com.externalAPIs.config;

import com.externalAPIs.store.ResponseStore;
import io.qameta.allure.Allure;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Unified API client for all HTTP verbs with:
 * - Retry for transient 5xx errors
 * - Integrated Allure step logging and attachments
 * - Automatic storage in ResponseStore for analytics
 */
public class ApiClient {

    /** Retries for transient 5xx (incl. Cloudflare 520/521) and overloaded origins. */
    private static final int MAX_RETRIES = 5;
    private static final AllureRestAssured allureFilter = new AllureRestAssured();

    /** POST request **/
    public static Response post(String url, Map<String, String> headers, Object body) {
        return executeWithRetry("POST", url, headers, body);
    }

    /** GET request **/
    public static Response get(String url, Map<String, String> headers) {
        return executeWithRetry("GET", url, headers, null);
    }

    /** PUT request **/
    public static Response put(String url, Map<String, String> headers, Object body) {
        return executeWithRetry("PUT", url, headers, body);
    }

    /** DELETE request **/
    public static Response delete(String url, Map<String, String> headers) {
        return executeWithRetry("DELETE", url, headers, null);
    }

    /**
     * Core retry mechanism for transient failures.
     */
    private static Response executeWithRetry(String method, String url, Map<String, String> headers, Object body) {
        Response response = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                RequestSpecification request = given()
                        .relaxedHTTPSValidation()
                        .headers(headers)
                        .filter(allureFilter) // 🔗 Integrate Allure RestAssured filter
                        .log().all();

                // 🧠 Set request body content type dynamically
                if (body != null) {
                    String contentType = headers.getOrDefault("Content-Type", "").toLowerCase();

                    if (contentType.contains("x-www-form-urlencoded") && body instanceof Map<?, ?> formData) {
                        request.contentType(ContentType.URLENC);
                        request.formParams((Map<String, ?>) formData);
                    } else {
                        request.contentType(ContentType.JSON);
                        request.body(body);
                    }
                }

                Allure.step("➡️ Sending " + method + " request to " + url);

                // 🔁 Execute request
                response = switch (method.toUpperCase()) {
                    case "POST" -> request.when().post(url).then().log().all().extract().response();
                    case "GET" -> request.when().get(url).then().log().all().extract().response();
                    case "PUT" -> request.when().put(url).then().log().all().extract().response();
                    case "DELETE" -> request.when().delete(url).then().log().all().extract().response();
                    default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
                };

                // 🧾 Store and attach request/response
                logRequestAndResponse(method, url, headers, body, response, attempt);

                // ✅ Exit loop for non-server errors
                if (response.statusCode() < 500) break;

                Allure.step("⚠️ Retryable server error " + response.statusCode() + " on attempt " + attempt);
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(400L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }

            } catch (Exception e) {
                Allure.step("❌ Exception on attempt " + attempt + ": " + e.getMessage());
                if (attempt == MAX_RETRIES) {
                    throw new RuntimeException("API call failed after " + MAX_RETRIES + " retries: " + e.getMessage(), e);
                }
            }
        }

        if (response == null) {
            throw new RuntimeException("No response received for " + method + " " + url);
        }
        return response;
    }

    /**
     * Centralized logging for Allure and ResponseStore.
     */
    private static void logRequestAndResponse(String method, String url, Map<String, String> headers,
                                              Object body, Response response, int attempt) {
        String maskedHeaders = maskSensitiveHeaders(headers);

        // Store in ResponseStore for listener attachments
        if (body != null) ResponseStore.put(method + "_RequestBody", body);
        ResponseStore.put(method + "_ResponseBody", response.asPrettyString());

        // Add Allure attachments
        try {
            Allure.addAttachment("📤 " + method + " Request (Attempt " + attempt + ")",
                    new ByteArrayInputStream(
                            (url + "\nHeaders:\n" + maskedHeaders + "\n\nBody:\n" + body)
                                    .getBytes(StandardCharsets.UTF_8)));

            Allure.addAttachment("📥 Response (" + response.statusCode() + ")",
                    new ByteArrayInputStream(
                            response.asPrettyString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            System.err.println("⚠️ Failed to attach request/response to Allure: " + e.getMessage());
        }
    }

    /**
     * Masks sensitive headers (tokens, authorization, etc.)
     */
    private static String maskSensitiveHeaders(Map<String, String> headers) {
        StringBuilder masked = new StringBuilder("{ ");
        headers.forEach((key, value) -> {
            if (key.toLowerCase().contains("authorization") || key.toLowerCase().contains("token")) {
                masked.append(key).append("=****, ");
            } else {
                masked.append(key).append("=").append(value).append(", ");
            }
        });
        return masked.append(" }").toString();
    }
}
