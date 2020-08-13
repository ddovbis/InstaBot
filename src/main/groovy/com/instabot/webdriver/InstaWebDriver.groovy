package com.instabot.webdriver

import com.instabot.config.InstaBotConfig
import com.instabot.data.model.primaryuser.PrimaryUser
import com.instabot.data.services.primaryuser.PrimaryUserDataService
import io.github.bonigarcia.wdm.DriverManagerType
import io.github.bonigarcia.wdm.WebDriverManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

import java.awt.*

@Component
class InstaWebDriver {
    private static final Logger LOG = LogManager.getLogger(InstaWebDriver.class)

    @Autowired
    private InstaBotConfig instaBotConfig
    @Autowired
    private PrimaryUserDataService primaryUserDataService

    private DriverManagerType driverManagerType
    public WebDriver driver
    public WebDriverWait wait
    public JavascriptExecutor jse
    public Actions actions

    public PrimaryUser primaryUser // TODO Remove and replace the usage by getOrCreate
    public String primaryUsername
    private String password

    @Bean("initializeInstaWebDriver")
    @DependsOn("initializeInstaBotConfig")
    private void initialize() {
        if (isInstaWebDriverDisabled()) {
            LOG.info("InstaWebDriver is disabled and will not be initialized")
            return
        }

        LOG.info("Initialize InstaWebDriver")
        initializePrimaryUser()
        setPrimaryUserPassword()
        initializeDriverManager()
        initializeWebDriver()
        initializeWebDriverWait()
        initializeJavaScriptExecutor()
        initializeActions()
        moveBrowserToCorrectMonitor()
        maximizeBrowserWindow()
        logIn()
        LOG.info("InstaWebDriver successfully initialized")
    }

    private isInstaWebDriverDisabled() {
        Boolean isDisabled = instaBotConfig.getIniFile().get("general", "disable-insta-web-driver", Boolean.class)
        if (isDisabled == null) {
            return false
        }
        return isDisabled
    }

    private void initializePrimaryUser() {
        LOG.info("Initialize primary user")
        primaryUsername = instaBotConfig.getIniFile().get("general", "primary-username", String.class)
        primaryUser = primaryUserDataService.createOrGetIfExists(primaryUsername)
        LOG.info("Primary user successfully initialized for the username: $primaryUsername")
    }

    private void setPrimaryUserPassword() {
        LOG.info("Set primary user's password")
        password = instaBotConfig.getIniFile().get("general", "password", String.class)
    }

    private void initializeDriverManager() {
        LOG.info("Initialize WebDriverManager")
        driverManagerType = DriverManagerType.valueOf(System.getProperty("webdriver.type"))
        WebDriverManager.getInstance(driverManagerType).setup()
        LOG.info("WebDriverManager Successfully initialized for driver manager type: $driverManagerType")
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

    private void initializeWebDriverWait() {
        wait = new WebDriverWait(driver, 10)
    }

    private void initializeJavaScriptExecutor() {
        jse = (JavascriptExecutor) driver
    }

    private void initializeActions() {
        actions = new Actions(driver)
    }

    private void moveBrowserToCorrectMonitor() {
        if (GraphicsEnvironment.isHeadless()) {
            return
        }

        String openBrowserOnDisplayNrStr = System.getProperty("open.browser.on.display.nr")
        LOG.info("Open browser on a specific monitor, based on system property open.browser.on.display.nr: $openBrowserOnDisplayNrStr")
        int openBrowserOnDisplayNr = openBrowserOnDisplayNrStr as int
        if (openBrowserOnDisplayNr < 0) {
            LOG.warn("Browser is set up to be opened on ${openBrowserOnDisplayNr}th display; the nr. is reset to 0")
            openBrowserOnDisplayNr = 0
        }

        GraphicsDevice[] screenDevices = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getScreenDevices()

        LOG.info("Total screen devices found: $screenDevices.length")

        if (openBrowserOnDisplayNr > screenDevices.length) {
            LOG.warn("Browser is set up to be open on ${openBrowserOnDisplayNr}th display, however, the total nr. of displays is $screenDevices.length; the nr. is reset to 0")
            openBrowserOnDisplayNr = 0
        }

        java.awt.Point displayLocation = screenDevices[openBrowserOnDisplayNr]
                .getDefaultConfiguration()
                .getBounds()
                .location

        driver.manage().window().setPosition(new Point(displayLocation.x as int, displayLocation.y as int))
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

    void closeConnection() {
        LOG.info("Close WebDriver connection")
        if (driver != null) {
            driver.close()
        }
    }
}

