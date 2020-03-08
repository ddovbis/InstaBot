package com.instabot.webdriver

import io.github.bonigarcia.wdm.DriverManagerType
import io.github.bonigarcia.wdm.WebDriverManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.ini4j.Wini
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

import java.awt.*

@Component
@DependsOn("loadSystemProperties")
public class InstaWebDriver {
    private static final Logger LOG = LogManager.getLogger(InstaWebDriver.class)

    private DriverManagerType driverManagerType
    public WebDriver driver
    public JavascriptExecutor jse;
    public WebDriverWait wait

    public String primaryUsername
    private String password

    public InstaWebDriver() {
        LOG.info("Initialize InstaWebDriver")
        extractCredentials()
        initializeDriverManager()
        initializeWebDriver()
        initializeWebDriverWait()
        initializeJavaScriptExecutor()
        moveBrowserToCorrectMonitor()
        maximizeBrowserWindow()
        logIn()
        LOG.info("InstaWebDriver successfully initialized for primary user: $primaryUsername")
    }

    private void initializeDriverManager() {
        LOG.info("Initialize WebDriverManager")
        driverManagerType = DriverManagerType.valueOf(System.getProperty("webdriver.type"))
        WebDriverManager.getInstance(driverManagerType).setup()
        LOG.info "WebDriverManager Successfully initialized for driver manager type: $driverManagerType"
    }

    private void extractCredentials() {
        LOG.info("Extract primary-user credentials")
        String configurationFilePath = System.getProperty("instabot.conf.path")
        File configurationFile = new File(configurationFilePath)
        if (configurationFile == null) {
            throw new FileNotFoundException("Could not find insta-bot configuration file by path: $configurationFilePath")
        }

        Wini ini = new Wini(configurationFile)
        primaryUsername = ini.get("general", "primary-username", String.class)
        password = ini.get("general", "password", String.class)
    }


    private void initializeWebDriver() {
        LOG.info("Initialize WebDriver")

        if (driverManagerType == DriverManagerType.CHROME) {
            driver = new ChromeDriver()
        } else if (driverManagerType == DriverManagerType.FIREFOX) {
            driver = new FirefoxDriver()
        } else {
            throw new IllegalArgumentException("Unsupported WebDriver type: $driverManagerType")
        }
    }

    private void initializeJavaScriptExecutor() {
        jse = (JavascriptExecutor) driver
    }

    private void initializeWebDriverWait() {
        wait = new WebDriverWait(driver, 10)
    }

    private void moveBrowserToCorrectMonitor() {
        if (GraphicsEnvironment.isHeadless()) {
            return
        }

        String openBrowserOnDisplayNrStr = System.getProperty("open.browser.on.display.nr")
        LOG.info "Open browser on a specific monitor, based on system property open.browser.on.display.nr: $openBrowserOnDisplayNrStr"
        int openBrowserOnDisplayNr = openBrowserOnDisplayNrStr as int
        if (openBrowserOnDisplayNr < 0) {
            LOG.warn("Browser is set up to be opened on ${openBrowserOnDisplayNr}th display; the nr. is reset to 0")
            openBrowserOnDisplayNr = 0
        }

        GraphicsDevice[] screenDevices = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getScreenDevices()

        if (openBrowserOnDisplayNr > screenDevices.length) {
            LOG.warn("Browser is set up to be open on ${openBrowserOnDisplayNr}th display, however, the total nr. of displays is $screenDevices.length; the nr. is reset to 0")
            openBrowserOnDisplayNr = 0
        }

        int upperLeftCornerOfTheMonitor = 0
        if (openBrowserOnDisplayNr > 1) {
            upperLeftCornerOfTheMonitor = (int) screenDevices[1]
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
        LOG.info("Log in to Instagram with primary user: $primaryUsername")

        // go to Instagram website
        driver.get("https://www.instagram.com/")
        // wait until "Log In" button appears, therefore the page is loaded
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[text()[contains(.,'Log in')]]")))

        // go to Log In page if sign up page opened (Instagram returns randomly sign up or log in page)
//        if (driver.findElements(By.xpath("//*[text() = 'Sign up']")).size() > 0) {
        if (driver.findElements(By.xpath("//button[text()='Sign up']")).size() > 0) {
            WebElement loginButton = driver.findElement(By.xpath("//*[text() = 'Log in']")) // xpath to login button
            loginButton.click()
            sleep(1000)
        }

        // put username and pawword and log in
        WebElement usernameElem = driver.findElement(By.name("username"))
        usernameElem.clear()
        usernameElem.sendKeys(primaryUsername)
        WebElement passwordElem = driver.findElement(By.name("password"))
        passwordElem.clear()
        passwordElem.sendKeys(password)
        passwordElem.sendKeys(Keys.RETURN)

        // wait until "Log In" button disappears, therefore the user is logged in
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//*[text()[contains(.,'Log In')]]")))
        LOG.info("User successfully logged in")
    }

    public closeConnection() {
        LOG.info("Close WebDriver connection")
        driver.close()
    }
}

