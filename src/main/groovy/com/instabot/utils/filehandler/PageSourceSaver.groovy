package com.instabot.utils.filehandler

import com.instabot.webdriver.InstaWebDriver
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import java.text.SimpleDateFormat

class PageSourceSaver {
    private static final Logger LOG = LogManager.getLogger(PageSourceSaver.class)

    static void savePageSourceOnException(InstaWebDriver instaDriver) {
        if (instaDriver == null) {
            LOG.warn("InstaWebDriver is null; no page source will be saved")
            return
        }

        String currentDateTime = new SimpleDateFormat("yyyyMMddHHmm").format(new Date())
        String fileName = "page_source_${currentDateTime}.html"
        String filePath = "./logs/error/$instaDriver.primaryUsername/$fileName"
        File file = new File(filePath)
        file.getParentFile().mkdirs()
        LOG.info("Save page source on exception to file: ${file.getAbsolutePath()}")
        file << instaDriver.driver.getPageSource()
    }
}
