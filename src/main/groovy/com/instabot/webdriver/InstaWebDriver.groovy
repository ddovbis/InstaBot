package com.instabot.webdriver

import org.apache.log4j.Logger
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

import java.awt.*

public class InstaWebDriver {
    private static final Logger LOG = Logger.getLogger(InstaWebDriver.class)

    public WebDriver driver
    public JavascriptExecutor jse
    public WebDriverWait wait

    private WEB_DRIVER_TYPE webDriverType
    private int openBrowserOnMonitorNr
    private String loginUsername
    private String loginPassword

    public InstaWebDriver(WEB_DRIVER_TYPE newWebDriverType, String newLoginUsername, String newLoginPassword, int newOpenBrowserOnMonitorNr) throws InterruptedException {
        webDriverType = newWebDriverType
        openBrowserOnMonitorNr = newOpenBrowserOnMonitorNr
        loginUsername = newLoginUsername
        loginPassword = newLoginPassword

        initiateWebDriver()
        initiateWebDriverWait()
        initiateJavaScriptExecutor()
        moveBrowserToCorrectMonitor()
        maximizeBrowserWindow()
        logIn()
    }

    private void initiateWebDriver() {
        LOG.info("Initialize WebDriver")
        if (webDriverType == WEB_DRIVER_TYPE.CHROME) {
            driver = new ChromeDriver()
        } else if (webDriverType == WEB_DRIVER_TYPE.FIREFOX) {
            driver = new FirefoxDriver()
        } else {
            throw new IllegalArgumentException("Unsupported WebDriver type: $webDriverType")
        }
    }

    private void initiateJavaScriptExecutor() {
        jse = (JavascriptExecutor) driver
    }

    private void initiateWebDriverWait() {
        wait = new WebDriverWait(driver, 10)
    }

    private void moveBrowserToCorrectMonitor() {
        int upperLeftCornerOfTheMonitor = 0
        if (openBrowserOnMonitorNr > 1) {
            upperLeftCornerOfTheMonitor = (int) GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getScreenDevices()[0]
                    .getDefaultConfiguration()
                    .getBounds()
                    .width
        }
        driver.manage().window().setPosition(new Point(upperLeftCornerOfTheMonitor, 0))
    }

    private void maximizeBrowserWindow() {
        driver.manage().window().maximize()
    }

    /**
     * Opens Instagram page and logs into user account passing username and password attributed to {@link InstaWebDriver} object
     */
    private void logIn() {
        // go to Instagram website
        driver.get("https://www.instagram.com/")
        // wait until "Log In" button appears, therefore the page is loaded
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[text()[contains(.,'Log in')]]")))

        // go to Log In page
        WebElement loginButton = driver.findElement(By.xpath("//*[text() = 'Log in']")) // xpath to login button
        loginButton.click()
        sleep(1000)

        // put username and pawword and log in
        WebElement usernameElem = driver.findElement(By.name("username"))
        usernameElem.clear()
        usernameElem.sendKeys(loginUsername)
        WebElement passwordElem = driver.findElement(By.name("password"))
        passwordElem.clear()
        passwordElem.sendKeys(loginPassword)
        passwordElem.sendKeys(Keys.RETURN)

        // wait until "Log In" button disappears, therefore the user is logged in
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//*[text()[contains(.,'Log In')]]")))
        LOG.info("User successfully logged in")
    }

    public isLoggedIn() {
        return !("Log In" in driver.getPageSource())
    }

}

enum WEB_DRIVER_TYPE {
    CHROME, FIREFOX
}
