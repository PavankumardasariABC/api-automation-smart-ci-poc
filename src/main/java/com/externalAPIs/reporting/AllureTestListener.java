package com.externalAPIs.reporting;

import com.externalAPIs.store.ResponseStore;
import io.qameta.allure.Allure;
import org.json.simple.JSONObject;
import org.testng.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AllureTestListener implements ITestListener, ISuiteListener {

    private static int passed = 0, failed = 0, skipped = 0;

    @Override
    public void onStart(ISuite suite) {
        try {
            ResponseStore.clear();
            System.out.println("🧹 ResponseStore cleared before suite run.");
        } catch (Exception e) {
            System.err.println("⚠️ Failed to clear ResponseStore: " + e.getMessage());
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        Allure.step("🧪 Test Started: " + result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        passed++;
        attachApiDetails(result);
        Allure.addAttachment("✅ Passed", result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        failed++;
        attachApiDetails(result);

        Throwable t = result.getThrowable();
        if (t != null) {
            Allure.addAttachment("❌ Failure Message",
                    new ByteArrayInputStream(t.toString().getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        skipped++;
        Allure.addAttachment("⚠️ Skipped", result.getMethod().getMethodName());
    }

    @Override
    public void onFinish(ISuite suite) {
        generateAnalyticsSummary();
        System.out.println("🏁 Test Suite Finished: " + suite.getName());
    }

    private void attachApiDetails(ITestResult result) {
        try {
            Object requestBody = ResponseStore.get("RequestBody");
            Object responseBody = ResponseStore.get("ResponseBody");

            if (requestBody != null)
                Allure.addAttachment("🟦 API Request (" + result.getMethod().getMethodName() + ")",
                        new ByteArrayInputStream(requestBody.toString().getBytes(StandardCharsets.UTF_8)));

            if (responseBody != null)
                Allure.addAttachment("🟩 API Response (" + result.getMethod().getMethodName() + ")",
                        new ByteArrayInputStream(responseBody.toString().getBytes(StandardCharsets.UTF_8)));

        } catch (Exception e) {
            System.err.println("⚠️ Failed to attach API details: " + e.getMessage());
        }
    }

    private void generateAnalyticsSummary() {
        try {
            int total = passed + failed + skipped;
            double passPercent = total == 0 ? 0 : (passed * 100.0 / total);

            JSONObject summary = new JSONObject();
            summary.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            summary.put("totalTests", total);
            summary.put("passed", passed);
            summary.put("failed", failed);
            summary.put("skipped", skipped);
            summary.put("passPercentage", String.format("%.2f", passPercent));

            File analyticsDir = new File("reports/analytics/");
            analyticsDir.mkdirs();
            try (FileWriter file = new FileWriter(new File(analyticsDir, "execution_summary.json"))) {
                file.write(summary.toJSONString());
            }
            System.out.println("✅ Analytics summary generated.");
        } catch (Exception e) {
            System.err.println("⚠️ Failed to generate analytics summary: " + e.getMessage());
        }
    }
}
