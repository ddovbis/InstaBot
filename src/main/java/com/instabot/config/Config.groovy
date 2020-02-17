package com.instabot.config

import org.apache.log4j.*
import org.openqa.selenium.firefox.FirefoxDriver

import java.text.SimpleDateFormat

class Config {
    private static final String LOG_PATTERN_S = "%d{yyyy-MM-dd HH:mm:ss} [%c{1}] %-5p - %m%n"
    private static final String LOG_PATTERN_MS = "%d{yyyy-MM-dd HH:mm:ss} [%c{1}] %-5p - %m%n"
    private static final String LOG_FILE_NAME_PATTERN = "${new SimpleDateFormat("yyyyMMdd").format(new Date())}.log"

    private static final Logger log = Logger.getLogger(Config.class)

    static init() {
        // TODO extract from .ini

        // extract and set up logger configuration
        String logLevel = "DEBUG"
        boolean millisecondsPrecision = false
        logger(logLevel, millisecondsPrecision)

        // extract and set up system configurations
        String pathToFirefoxDriver = ".\\resources\\drivers\\geckodriver.exe"
        boolean enableFirefoxLogs = false
        String pathToChromeDriver = ".\\resources\\drivers\\chromedriver.exe"
        systemParameters(pathToFirefoxDriver, enableFirefoxLogs, pathToChromeDriver)
    }

    private static logger(String logLevel, boolean millisecondsPrecision) {
        // extract root logger to set configurations for all descendant loggers
        Logger rootLogger = Logger.getRootLogger()

        // reset default configuration
        rootLogger.getLoggerRepository().resetConfiguration()

        // set log level to the one provided in .ini or to DEBUG if the parameter is missing
        rootLogger.setLevel(Level.toLevel(logLevel, Level.DEBUG))

        // crate pattern layout w/ or w/o milliseconds precision
        PatternLayout patternLayout = new PatternLayout(millisecondsPrecision ? LOG_PATTERN_MS : LOG_PATTERN_S)

        // setup console configuration and add it to root logger
        ConsoleAppender console = new ConsoleAppender()
        console.setLayout(patternLayout)
        console.activateOptions()
        rootLogger.addAppender(console)

        // setup writing to file configuration and add it to root logger
        FileAppender fa = new FileAppender()
        fa.setName("FileLogger")
        fa.setFile("./logs/$LOG_FILE_NAME_PATTERN")
        fa.setLayout(patternLayout)
        fa.setAppend(true)
        fa.activateOptions()
        rootLogger.addAppender(fa);

        log.info "Logger successfully initialized"
    }

    private static systemParameters(String pathToFirefoxDriver, boolean enableFirefoxLogs, String pathToChromeDriver) {
        // set up Firefox driver
        if (pathToFirefoxDriver != null) {
            // add path to driver
            log.info("Initialize Firefox driver")
            System.setProperty("webdriver.gecko.driver", pathToFirefoxDriver)

            // suppress Firefox startup logs if not enabled explicitly
            if (!enableFirefoxLogs) {
                log.info("Disable Firefox logs")
                System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null")
            }
        }

        // set up Chrome driver
        if (pathToFirefoxDriver != null) {
            // add path to driver
            log.info("Initialize Chrome driver")
            System.setProperty("webdriver.chrome.driver", pathToChromeDriver)
        }
    }

}
