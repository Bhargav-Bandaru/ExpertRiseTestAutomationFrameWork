package com.expertrise.automation.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExtentReportManager — thread-safe singleton for Extent Reports HTML dashboard.
 *
 * <p>Creates one ExtentReports instance per test run (initialised on first use)
 * and one ExtentTest per Cucumber scenario. Uses ThreadLocal so parallel threads
 * do not overwrite each other's test nodes.</p>
 *
 * <p>Called from {@link com.expertrise.automation.hooks.Hooks}:
 * <pre>
 *   // In @Before
 *   ExtentReportManager.createTest(scenario.getName(), tags);
 *
 *   // In @After
 *   ExtentReportManager.passTest(scenario.getName());    // or failTest()
 *   ExtentReportManager.flush();
 * </pre>
 */
public class ExtentReportManager {

    private static final Logger log = LogManager.getLogger(ExtentReportManager.class);

    private static ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> testThread = new ThreadLocal<>();
    // Map scenario name → ExtentTest for cross-thread lookup
    private static final ConcurrentHashMap<String, ExtentTest> testMap = new ConcurrentHashMap<>();

    private static final String REPORT_DIR  = "target/extent-reports/";
    private static final String REPORT_NAME = "ExpertRise_Test_Report_"
            + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".html";

    // ──────────────────────────────────────────────────────────────────────────
    // INITIALISE
    // ──────────────────────────────────────────────────────────────────────────

    private static synchronized void init() {
        if (extentReports == null) {
            new File(REPORT_DIR).mkdirs();
            String reportPath = REPORT_DIR + REPORT_NAME;

            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setDocumentTitle("ExpertRise Automation Report");
            spark.config().setReportName("BDD Test Execution — Selenium + Playwright + API");
            spark.config().setTheme(Theme.DARK);
            spark.config().setEncoding("UTF-8");

            extentReports = new ExtentReports();
            extentReports.attachReporter(spark);
            extentReports.setSystemInfo("Project",     "ExpertRise BDD Framework");
            extentReports.setSystemInfo("Environment", System.getProperty("env", "QA"));
            extentReports.setSystemInfo("Browser",     System.getProperty("browser", "chrome"));
            extentReports.setSystemInfo("Java",        System.getProperty("java.version"));
            extentReports.setSystemInfo("OS",          System.getProperty("os.name"));

            log.info("Extent Reports initialised — report: {}", reportPath);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // TEST LIFECYCLE
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new test node in the Extent Report for the given scenario.
     *
     * @param scenarioName   Cucumber scenario name
     * @param tags           comma-separated tags string
     */
    public static synchronized void createTest(String scenarioName, String tags) {
        if (extentReports == null) init();
        ExtentTest test = extentReports.createTest(scenarioName);
        if (tags != null && !tags.isBlank()) {
            for (String tag : tags.split(",")) {
                test.assignCategory(tag.trim().replace("@", ""));
            }
        }
        testThread.set(test);
        testMap.put(scenarioName, test);
        log.debug("Extent test created for: {}", scenarioName);
    }

    /**
     * Returns the ExtentTest for the current thread.
     */
    public static ExtentTest getTest() {
        return testThread.get();
    }

    /**
     * Marks the scenario as PASSED.
     */
    public static void passTest(String scenarioName) {
        ExtentTest test = testMap.get(scenarioName);
        if (test != null) {
            test.log(Status.PASS, "Scenario PASSED ✅");
        }
    }

    /**
     * Marks the scenario as FAILED.
     */
    public static void failTest(String scenarioName) {
        ExtentTest test = testMap.get(scenarioName);
        if (test != null) {
            test.log(Status.FAIL, "Scenario FAILED ❌");
        }
    }

    /**
     * Attaches a screenshot (as base64) to the failed test's report node.
     *
     * @param scenarioName scenario name key
     * @param screenshot   PNG bytes from TakesScreenshot or Playwright
     */
    public static void attachScreenshotOnFailure(String scenarioName, byte[] screenshot) {
        ExtentTest test = testMap.get(scenarioName);
        if (test != null && screenshot != null) {
            try {
                String base64 = java.util.Base64.getEncoder().encodeToString(screenshot);
                test.addScreenCaptureFromBase64String(base64, "Failure Screenshot");
                log.debug("Screenshot attached for: {}", scenarioName);
            } catch (Exception e) {
                log.warn("Failed to attach screenshot: {}", e.getMessage());
            }
        }
    }

    /**
     * Logs a custom step with INFO status in the report.
     *
     * @param message step description
     */
    public static void logStep(String message) {
        ExtentTest test = testThread.get();
        if (test != null) test.log(Status.INFO, message);
    }

    /**
     * Flushes the report — writes all in-memory data to the HTML file.
     * Must be called at end of suite or after each scenario in Hooks @After.
     */
    public static synchronized void flush() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }
}
