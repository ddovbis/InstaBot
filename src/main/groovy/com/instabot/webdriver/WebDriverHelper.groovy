package com.instabot.webdriver

import com.paulhammant.ngwebdriver.ByAngular
import com.paulhammant.ngwebdriver.NgWebDriver
import org.apache.commons.lang3.reflect.FieldUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.openqa.selenium.By
import org.openqa.selenium.ElementClickInterceptedException
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.ScriptTimeoutException
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class WebDriverHelper {
    private static final Logger LOG = LogManager.getLogger(WebDriverHelper.class)

    @Autowired
    private InstaWebDriver instaDriver

    // --- Methods used to check if the page is loaded ---

    /**
     * Waits for page to load in multiple with multiple attempts
     *
     * Note: It looks like the NgWebDriver's waitForAngularRequestsToFinish method gets stale in some cases
     * (e.g., after something has just been downloaded), this is why it is necessary to loop try/catch block, in order
     * to allow it to recheck the page loading processes, even if it fails initially.
     */
    void waitForPageToLoad() {
        LOG.debug("Wait for the page to load")
        for (int i = 1; i <= 5; i++) {
            try {
                instaDriver.wait2sec.until({ isPageLoaded() })
                LOG.debug("The page has been loaded")
                return
            } catch (ScriptTimeoutException ste) {
                if (i < 5) {
                    LOG.warn("The page could not be loaded yet, trying again (attempt $i out of 5)")
                } else {
                    LOG.error("The page could not be loaded after 5 attempts with a timeout of 2 seconds each")
                    throw ste
                }
            }
        }
    }

    boolean isPageLoaded() {
        return instaDriver.jse.executeScript("return document.readyState") == "complete"
    }

    void refreshPage() {
        instaDriver.driver.navigate().refresh()
        waitForPageToLoad()
    }

    // --- Methods used to work with Selenium web elements ---

    void scrollToElement(WebElement element) {
        instaDriver.jse.executeScript("arguments[0].scrollIntoView(true);", element)
    }

    boolean existsOnPage(WebElement element) {
        return element == null ? false : !instaDriver.driver.findElements(getByLocatorOf(element)).empty
    }

    static boolean existsInElement(WebElement parentElement, WebElement childElement) {
        return (childElement == null || parentElement == null) ? false : !parentElement.findElements(getByLocatorOf(childElement)).empty
    }

    static boolean existsInElement(WebElement parentElement, String childElementCssSelector) {
        return (childElementCssSelector == null || parentElement == null) ? false : !getAllByCssSelector(parentElement, childElementCssSelector).empty
    }

    static By getByLocatorOf(WebElement element) {
        Object proxyOrigin = FieldUtils.readField(element, "h", true)
        Object locator = FieldUtils.readField(proxyOrigin, "locator", true)
        Object findBy = FieldUtils.readField(locator, "by", true)

        if (findBy == null || !(findBy instanceof By)) {
            throw new NoSuchElementException("Could not find by locator of the WebElement: $element")
        }
        return findBy as By
    }

    boolean isElementDisplayedByCssSelector(String cssSelector) {
        return getByCssSelector(cssSelector).isDisplayed()
    }

    boolean isDisplayed(WebElement element) {
        waitForPageToLoad()
        try {
            return element.isDisplayed()
        } catch (StaleElementReferenceException ignore) {
            // if this exception is thrown, the DOM has already been changed
            // try checking the element once again, refreshing it first
            WebElement refreshedElement = getRefreshedElement(element)
            if (refreshedElement) {
                return refreshedElement.isDisplayed()
            } else {
                return false
            }
        } catch (NoSuchElementException ignore) {
            return false
        }
    }

    boolean isClickable(WebElement element) {
        waitForPageToLoad()

        try {
            instaDriver.wait1sec.until(ExpectedConditions.elementToBeClickable(element))
            return true
        } catch (TimeoutException | NullPointerException ignore) {
            return false
        }
    }

    WebElement getRefreshedElement(WebElement element) {
        try {
            return instaDriver.driver.findElement(getByLocatorOf(element))
        } catch (NoSuchElementException ignore) {
            return null
        }
    }

    WebElement getByXPath(String xPath) {
        try {
            return instaDriver.driver.findElement(By.xpath(xPath))
        } catch (NoSuchElementException ignore) {
            return null
        }
    }

    static WebElement getByXPath(WebElement element, String xPath) {
        try {
            return element.findElement(By.xpath(xPath))
        } catch (NoSuchElementException ignore) {
            return null
        }
    }

    List<WebElement> getAllByXPath(String xPath) {
        return instaDriver.driver.findElements(By.xpath(xPath))
    }

    static List<WebElement> getAllByXPath(WebElement element, String xPath) {
        return element.findElements(By.xpath(xPath))
    }

    WebElement getByTagName(String tagName) {
        try {
            return instaDriver.driver.findElement(By.tagName(tagName))
        } catch (NoSuchElementException ignore) {
            return null
        }
    }

    static WebElement getByTagName(WebElement element, String tagName) {
        try {
            return element.findElement(By.tagName(tagName))
        } catch (NoSuchElementException ignore) {
            return null
        }
    }

    List<WebElement> getAllByTagName(String tagName) {
        return instaDriver.driver.findElements(By.tagName(tagName))
    }

    static List<WebElement> getAllByTagName(WebElement element, String tagName) {
        return element.findElements(By.tagName(tagName))
    }

    WebElement getByCssSelector(String cssSelector) {
        try {
            return instaDriver.driver.findElement(By.cssSelector(cssSelector))
        } catch (NoSuchElementException ignore) {
            return null
        }
    }

    static WebElement getByCssSelector(WebElement element, String cssSelector) {
        try {
            return element.findElement(By.cssSelector(cssSelector))
        } catch (NoSuchElementException ignore) {
            return null
        }
    }

    List<WebElement> getAllByCssSelector(String cssSelector) {
        return instaDriver.driver.findElements(By.cssSelector(cssSelector))
    }

    static List<WebElement> getAllByCssSelector(WebElement element, String cssSelector) {
        return element.findElements(By.cssSelector(cssSelector))
    }

    WebElement getByCssContainingText(String cssSelector, String text) {
        try {
            return instaDriver.driver.findElement(ByAngular.cssContainingText(cssSelector, text))
        } catch (NoSuchElementException ignore) {
            return null
        }
    }

    static WebElement getByCssContainingText(WebElement element, String cssSelector, String text) {
        try {
            return element.findElement(ByAngular.cssContainingText(cssSelector, text))
        } catch (NoSuchElementException ignore) {
            return null
        }
    }

    List<WebElement> getAllByCssContainingText(String cssSelector, String text) {
        return instaDriver.driver.findElements(ByAngular.cssContainingText(cssSelector, text))
    }

    static List<WebElement> getAllByCssContainingText(WebElement element, String cssSelector, String text) {
        return element.findElements(ByAngular.cssContainingText(cssSelector, text))
    }

    WebElement getByButtonText(String text) {
        try {
            return instaDriver.driver.findElement(ByAngular.buttonText(text))
        } catch (NoSuchElementException ignore) {
            return null
        }
    }

    List<WebElement> getAllByButtonText(String text) {
        return instaDriver.driver.findElements(ByAngular.buttonText(text))
    }

    /**
     * Does the following actions:
     *  - Scrolls into view if web-element is hidden in it
     *  - Waits for the web-element to become clickable (to cover cases when the view is loading after scrolling)
     *  - Clicks on the web-element
     *
     * @param element - web-element to click on
     */
    void click(WebElement element) {
        scrollToElement(element)
        waitToBeClickable(element)

        try {
            element.click()
        } catch (ElementClickInterceptedException ignore) {
            LOG.warn("Element click intercepted, trying to click via JavasScriptExecutor on element: ${getHtml(element)}")
            clickWithJs(element)
        }

    }

    void clickWithJs(WebElement element) {
        instaDriver.jse.executeScript("arguments[0].click();", element);
    }

    void waitToBeClickable(WebElement element) {
        instaDriver.wait2sec.until(ExpectedConditions.elementToBeClickable(element))
    }

    /**
     * Does the following actions:
     *  - Scrolls into view if web-element is hidden in it
     *  - Waits for the web-element to become clickable (to cover cases when the view is loading after scrolling)
     *  - Clears the web-element from the previously added text (if any)
     *  - Sends the provided text to the web-element
     *
     * @param element - web-element to send text to
     * @param text - text to send to the web-element
     */
    void sendKeysWithClear(WebElement element, String text) {
        scrollToElement(element)
        instaDriver.wait2sec.until(ExpectedConditions.elementToBeClickable(element))
        instaDriver.actions.moveToElement(element).click().perform()
        element.clear()
        element.sendKeys(text)
    }

    static String getHtml(WebElement element) {
        return element?.getAttribute("outerHTML")
    }

    // --- Methods used to work with with web-elements via Jsoup document and elements ---

    /**
     * @param webElement - selenium web-element
     * @return - Jsoup equivalent of the provided web-element
     */
    static Element getJsoupElement(WebElement webElement) {
        return Jsoup.parse(webElement.getAttribute("outerHTML"), "", Parser.xmlParser()).child(0)
    }

    static String getOwnText(WebElement webElement) {
        return getJsoupElement(webElement).ownText()
    }
}
