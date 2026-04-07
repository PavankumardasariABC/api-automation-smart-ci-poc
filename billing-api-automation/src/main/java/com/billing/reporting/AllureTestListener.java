package com.billing.reporting;

import com.billing.store.ResponseStore;
import io.qameta.allure.Allure;
import org.json.simple.JSONObject;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AllureTestListener implements ITestListener, ISuiteListener {

    private static int passed;
    private static int failed;
    private static int skipped;

    @Override
    public void onStart(ISuite suite) {
        try {
            ResponseStore.clear();
        } catch (Exception e) {
            System.err.println("ResponseStore clear failed: " + e.getMessage());
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        Allure.step("Test started: " + result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        passed++;
        attachApiDetails(result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        failed++;
        attachApiDetails(result);
        Throwable t = result.getThrowable();
        if (t != null) {
            Allure.addAttachment("Failure",
                    new ByteArrayInputStream(t.toString().getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        skipped++;
    }

    @Override
    public void onFinish(ISuite suite) {
        writeSummary();
    }

    private static void attachApiDetails(ITestResult result) {
        try {
            Object req = ResponseStore.get("RequestBody");
            Object res = ResponseStore.get("ResponseBody");
            if (req != null) {
                Allure.addAttachment("API Request (" + result.getMethod().getMethodName() + ")",
                        new ByteArrayInputStream(req.toString().getBytes(StandardCharsets.UTF_8)));
            }
            if (res != null) {
                Allure.addAttachment("API Response (" + result.getMethod().getMethodName() + ")",
                        new ByteArrayInputStream(res.toString().getBytes(StandardCharsets.UTF_8)));
            }
        } catch (Exception e) {
            System.err.println("attachApiDetails failed: " + e.getMessage());
        }
    }

    private static void writeSummary() {
        try {
            int total = passed + failed + skipped;
            double pct = total == 0 ? 0 : (passed * 100.0 / total);
            JSONObject o = new JSONObject();
            o.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            o.put("totalTests", total);
            o.put("passed", passed);
            o.put("failed", failed);
            o.put("skipped", skipped);
            o.put("passPercentage", String.format("%.2f", pct));
            File dir = new File("reports/analytics/");
            dir.mkdirs();
            try (FileWriter fw = new FileWriter(new File(dir, "execution_summary.json"))) {
                fw.write(o.toJSONString());
            }
        } catch (Exception e) {
            System.err.println("Analytics summary failed: " + e.getMessage());
        }
    }
}
