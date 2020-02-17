package com.instabot

import com.instabot.config.Config
import org.apache.log4j.Logger

import java.awt.*

class Main {
    private static final Logger LOG = Logger.getLogger(Main.class)

    public static void main(String[] str) throws InterruptedException, AWTException {
        // set up configuration based on default setup and InstaBot.ini file
        Config.init()

        // start process
        new InstaBot().start()

        LOG.info "InstaBot execution finished"
    }
}
