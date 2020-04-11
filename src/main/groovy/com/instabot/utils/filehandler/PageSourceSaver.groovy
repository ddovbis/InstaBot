package com.instabot.utils.filehandler

import com.instabot.webdriver.InstaWebDriver
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import java.time.LocalDateTime

class PageSourceSaver {
    private static final Logger LOG = LogManager.getLogger(PageSourceSaver.class)

    static void savePageSourceOnException(InstaWebDriver instaDriver) {
        if (instaDriver == null) {
            LOG.warn("InstaWebDriver is null; no page source will be saved")
            return
        }

        String fileName = "page_source_${LocalDateTime.now().format("yyyyMMddmmss")}.html"
        String filePath = "./logs/error/$instaDriver.primaryUsername/$fileName"
        File file = new File(filePath)
        file.getParentFile().mkdirs()
        LOG.info("Save page source on exception to file: ${file.getAbsolutePath()}")
        file << instaDriver.driver.getPageSource()
    }
}
