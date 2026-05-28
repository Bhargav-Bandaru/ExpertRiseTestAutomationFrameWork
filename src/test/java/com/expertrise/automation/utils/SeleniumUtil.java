package com.expertrise.automation.utils;

import com.expertrise.automation.config.DriverFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SeleniumUtil — complete reusable Selenium WebDriver helper library.
 *
 * Categories covered:
 *   Click           — click, jsClick, doubleClick, rightClick
 *   Input           — typeText, clearField, appendText, pressKey
 *   Dropdown        — selectByVisibleText/Value/Index, getAllDropdownOptions
 *   Frame           — switchToFrame, switchToDefaultContent, switchToParentFrame
 *   Alert           — acceptAlert, dismissAlert, getAlertText, typeInAlert
 *   Window/Tab      — switchToNewWindow, switchToWindow, getWindowCount
 *   Scroll          — scrollIntoView, scrollToTop, scrollToBottom, scrollByPixels
 *   Drag-Drop       — dragAndDrop, dragAndDropByOffset
 *   Hover/Mouse     — hoverOverElement, hoverAndClick
 *   State checks    — isDisplayed, isEnabled, isSelected, isPresent
 *   Text/Attribute  — getText, getAttribute, getInputValue, getAllTexts, getElementCount
 *   JavaScript      — jsClick, executeScript, highlightElement, setValueByJS, waitForPageLoad
 */
public class SeleniumUtil {

    private static final Logger log = LogManager.getLogger(SeleniumUtil.class);
    private static final int DEFAULT_WAIT = 15;

    private static WebDriver driver()  { return DriverFactory.getDriver(); }
    private static JavascriptExecutor js() { return (JavascriptExecutor) driver(); }
    private static WebDriverWait wait() {
        return new WebDriverWait(driver(), Duration.ofSeconds(DEFAULT_WAIT));
    }
    private static WebElement waitClickable(By by) {
        return wait().until(ExpectedConditions.elementToBeClickable(by));
    }
    private static WebElement waitVisible(By by) {
        return wait().until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    // ── CLICK HELPERS ──────────────────────────────────────────────────────────

    /** Standard click — waits for element to be clickable first. */
    public static void clickElement(By locator) {
        log.info("Click: {}", locator);
        waitClickable(locator).click();
    }

    /** JavaScript click — bypasses overlays/interceptors. Use as last resort. */
    public static void jsClick(By locator) {
        log.info("JS Click: {}", locator);
        js().executeScript("arguments[0].click();", waitVisible(locator));
    }

    /** Double-click using Actions class. */
    public static void doubleClick(By locator) {
        log.info("Double-click: {}", locator);
        new Actions(driver()).doubleClick(waitClickable(locator)).perform();
    }

    /** Right-click (context menu) using Actions class. */
    public static void rightClick(By locator) {
        log.info("Right-click: {}", locator);
        new Actions(driver()).contextClick(waitVisible(locator)).perform();
    }

    // ── INPUT HELPERS ──────────────────────────────────────────────────────────

    /** Clear existing value then type new text into an input field. */
    public static void typeText(By locator, String text) {
        log.info("Type '{}' → {}", text, locator);
        WebElement el = waitVisible(locator);
        el.clear();
        el.sendKeys(text);
    }

    /** Append text without clearing first. */
    public static void appendText(By locator, String text) {
        waitVisible(locator).sendKeys(text);
    }

    /**
     * Clear a field using Ctrl+A then Delete — more reliable than clear()
     * for React/Angular components that ignore element.clear().
     */
    public static void clearField(By locator) {
        log.info("Clear field: {}", locator);
        WebElement el = waitVisible(locator);
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.DELETE);
    }

    /** Send a specific keyboard key to an element. Example: Keys.ENTER, Keys.TAB. */
    public static void pressKey(By locator, Keys key) {
        log.info("Press {} on: {}", key, locator);
        waitVisible(locator).sendKeys(key);
    }

    // ── DROPDOWN HELPERS ───────────────────────────────────────────────────────

    /** Select &lt;select&gt; dropdown option by visible text. */
    public static void selectByVisibleText(By locator, String text) {
        log.info("Select by text '{}' from: {}", text, locator);
        new Select(waitVisible(locator)).selectByVisibleText(text);
    }

    /** Select &lt;select&gt; dropdown option by value attribute. */
    public static void selectByValue(By locator, String value) {
        log.info("Select by value '{}' from: {}", value, locator);
        new Select(waitVisible(locator)).selectByValue(value);
    }

    /** Select &lt;select&gt; dropdown option by zero-based index. */
    public static void selectByIndex(By locator, int index) {
        log.info("Select by index {} from: {}", index, locator);
        new Select(waitVisible(locator)).selectByIndex(index);
    }

    /** Get the currently selected option text from a dropdown. */
    public static String getSelectedOption(By locator) {
        return new Select(waitVisible(locator)).getFirstSelectedOption().getText();
    }

    /** Get all option texts from a dropdown as a List. */
    public static List<String> getAllDropdownOptions(By locator) {
        List<String> texts = new ArrayList<>();
        new Select(waitVisible(locator)).getOptions().forEach(o -> texts.add(o.getText().trim()));
        return texts;
    }

    // ── FRAME HELPERS ──────────────────────────────────────────────────────────

    /** Switch into an iframe — waits until frame is available. */
    public static void switchToFrame(By frameLocator) {
        log.info("Switch to frame: {}", frameLocator);
        wait().until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameLocator));
    }

    /** Switch into an iframe by name or id attribute. */
    public static void switchToFrameByNameOrId(String nameOrId) {
        log.info("Switch to frame by name/id: {}", nameOrId);
        wait().until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(nameOrId));
    }

    /** Switch into an iframe by zero-based index. */
    public static void switchToFrameByIndex(int index) {
        driver().switchTo().frame(index);
    }

    /** Exit all frames — return to main page context. */
    public static void switchToDefaultContent() {
        log.info("Switch to default content");
        driver().switchTo().defaultContent();
    }

    /** Switch to parent frame (one level up from nested frame). */
    public static void switchToParentFrame() {
        driver().switchTo().parentFrame();
    }

    // ── ALERT HELPERS ──────────────────────────────────────────────────────────

    /** Wait for alert then accept it (click OK). */
    public static void acceptAlert() {
        log.info("Accept alert");
        wait().until(ExpectedConditions.alertIsPresent()).accept();
    }

    /** Wait for alert then dismiss it (click Cancel). */
    public static void dismissAlert() {
        log.info("Dismiss alert");
        wait().until(ExpectedConditions.alertIsPresent()).dismiss();
    }

    /** Get the text shown inside the current alert dialog. */
    public static String getAlertText() {
        String text = wait().until(ExpectedConditions.alertIsPresent()).getText();
        log.info("Alert text: '{}'", text);
        return text;
    }

    /** Type text into a prompt alert then accept it. */
    public static void typeInAlert(String text) {
        log.info("Type in alert: {}", text);
        Alert alert = wait().until(ExpectedConditions.alertIsPresent());
        alert.sendKeys(text);
        alert.accept();
    }

    // ── WINDOW / TAB HELPERS ───────────────────────────────────────────────────

    /** Get the current browser window handle. */
    public static String getCurrentWindowHandle() {
        return driver().getWindowHandle();
    }

    /** Switch to the newly opened window/tab (not the original handle). */
    public static void switchToNewWindow(String originalHandle) {
        log.info("Switch to new window");
        for (String handle : driver().getWindowHandles()) {
            if (!handle.equals(originalHandle)) {
                driver().switchTo().window(handle);
                return;
            }
        }
        throw new RuntimeException("No new window found to switch to");
    }

    /** Switch to a specific window by its handle string. */
    public static void switchToWindow(String windowHandle) {
        driver().switchTo().window(windowHandle);
    }

    /** Close current window and switch back to the original. */
    public static void closeCurrentWindowAndSwitch(String originalHandle) {
        driver().close();
        driver().switchTo().window(originalHandle);
        log.info("Closed tab and returned to original window");
    }

    /** Return count of open browser windows/tabs. */
    public static int getWindowCount() {
        return driver().getWindowHandles().size();
    }

    // ── SCROLL HELPERS ─────────────────────────────────────────────────────────

    /** Scroll element into the centre of the viewport. */
    public static void scrollIntoView(By locator) {
        log.info("Scroll into view: {}", locator);
        js().executeScript("arguments[0].scrollIntoView({block:'center'});",
                           driver().findElement(locator));
    }

    /** Scroll a WebElement you already hold into the viewport. */
    public static void scrollIntoView(WebElement element) {
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    /** Scroll to the very top of the page. */
    public static void scrollToTop() {
        js().executeScript("window.scrollTo(0,0);");
    }

    /** Scroll to the very bottom of the page. */
    public static void scrollToBottom() {
        js().executeScript("window.scrollTo(0,document.body.scrollHeight);");
    }

    /** Scroll down by a specific number of pixels. */
    public static void scrollByPixels(int pixels) {
        js().executeScript("window.scrollBy(0," + pixels + ");");
    }

    // ── DRAG AND DROP ─────────────────────────────────────────────────────────

    /** Drag source element and drop onto target element. */
    public static void dragAndDrop(By sourceLocator, By targetLocator) {
        log.info("Drag {} → drop {}", sourceLocator, targetLocator);
        new Actions(driver())
            .dragAndDrop(waitVisible(sourceLocator), waitVisible(targetLocator))
            .perform();
    }

    /** Drag element by x/y pixel offset from its current position. */
    public static void dragAndDropByOffset(By sourceLocator, int xOffset, int yOffset) {
        log.info("Drag {} by ({},{})", sourceLocator, xOffset, yOffset);
        new Actions(driver())
            .dragAndDropBy(waitVisible(sourceLocator), xOffset, yOffset)
            .perform();
    }

    // ── HOVER / MOUSE ─────────────────────────────────────────────────────────

    /** Hover the mouse over an element (triggers CSS :hover and mouseover events). */
    public static void hoverOverElement(By locator) {
        log.info("Hover: {}", locator);
        new Actions(driver()).moveToElement(waitVisible(locator)).perform();
    }

    /** Hover over one element then click another (e.g. mega-menu navigation). */
    public static void hoverAndClick(By hoverLocator, By clickLocator) {
        log.info("Hover {} → click {}", hoverLocator, clickLocator);
        new Actions(driver())
            .moveToElement(waitVisible(hoverLocator))
            .click(waitClickable(clickLocator))
            .perform();
    }

    // ── STATE VERIFIERS ────────────────────────────────────────────────────────

    /** True if element is visible on screen. */
    public static boolean isDisplayed(By locator) {
        try { return driver().findElement(locator).isDisplayed(); }
        catch (NoSuchElementException e) { return false; }
    }

    /** True if element is enabled (not disabled/greyed out). */
    public static boolean isEnabled(By locator) {
        try { return driver().findElement(locator).isEnabled(); }
        catch (NoSuchElementException e) { return false; }
    }

    /** True if checkbox or radio button is checked/selected. */
    public static boolean isSelected(By locator) {
        try { return driver().findElement(locator).isSelected(); }
        catch (NoSuchElementException e) { return false; }
    }

    /** True if element is present in DOM (even if hidden). */
    public static boolean isPresent(By locator) {
        return !driver().findElements(locator).isEmpty();
    }

    // ── TEXT / ATTRIBUTE GETTERS ───────────────────────────────────────────────

    /** Get visible text of an element. */
    public static String getText(By locator) {
        return waitVisible(locator).getText().trim();
    }

    /** Get any HTML attribute value. Example: getAttribute(By.id("btn"), "class") */
    public static String getAttribute(By locator, String attribute) {
        return waitVisible(locator).getAttribute(attribute);
    }

    /** Get the value attribute of an input field. */
    public static String getInputValue(By locator) {
        return getAttribute(locator, "value");
    }

    /** Get the current page title. */
    public static String getPageTitle() {
        return driver().getTitle();
    }

    /** Get the current page URL. */
    public static String getCurrentUrl() {
        return driver().getCurrentUrl();
    }

    /** Get text of all matching elements (e.g. all table rows, all list items). */
    public static List<String> getAllTexts(By locator) {
        List<String> texts = new ArrayList<>();
        driver().findElements(locator).forEach(e -> texts.add(e.getText().trim()));
        return texts;
    }

    /** Count of elements matching a locator. */
    public static int getElementCount(By locator) {
        return driver().findElements(locator).size();
    }

    // ── JAVASCRIPT EXECUTOR ────────────────────────────────────────────────────

    /** Execute arbitrary JavaScript. Returns script return value. */
    public static Object executeScript(String script, Object... args) {
        return js().executeScript(script, args);
    }

    /** Highlight element with red border + yellow background for debugging. */
    public static void highlightElement(By locator) {
        js().executeScript(
            "arguments[0].style.border='3px solid red';" +
            "arguments[0].style.background='yellow';",
            driver().findElement(locator));
    }

    /**
     * Set an input value via JavaScript — works for readonly fields,
     * date pickers, and React/Angular controlled inputs.
     */
    public static void setValueByJS(By locator, String value) {
        log.info("JS setValue '{}' → {}", value, locator);
        js().executeScript("arguments[0].value='" + value + "';",
                           driver().findElement(locator));
    }

    /** Get an input field value via JavaScript (reads actual DOM value). */
    public static String getValueByJS(By locator) {
        return (String) js().executeScript("return arguments[0].value;",
                                           driver().findElement(locator));
    }

    /**
     * Wait for document.readyState === 'complete'.
     * Call after navigation to ensure full page load before interaction.
     */
    public static void waitForPageLoad() {
        new WebDriverWait(driver(), Duration.ofSeconds(DEFAULT_WAIT)).until(
            d -> js().executeScript("return document.readyState").equals("complete"));
    }

    private SeleniumUtil() {}
}
