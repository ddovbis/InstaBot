package com.instabot

import com.instabot.operators.weboperations.userextractor.RelatedUsersUpdater
import com.instabot.webdriver.InstaWebDriver
import com.instabot.webdriver.WEB_DRIVER_TYPE

import java.awt.*

class InstaBot {
    private InstaWebDriver instaDriver

    InstaBot() throws AWTException {
        instaDriver = new InstaWebDriver(WEB_DRIVER_TYPE.FIREFOX, "amidov_traveling", "565565", 2)
    }

    void start() throws InterruptedException {
        /*
        extract all followers and followed users of the master user and report them by following categories:
        - follower
        - followed
        - followers that are not followed
        - followed that don't follow back
         */

        RelatedUsersUpdater.updateRelatedUsers(instaDriver, "lina_alexandrean")
        instaDriver.driver.close()
    }
}