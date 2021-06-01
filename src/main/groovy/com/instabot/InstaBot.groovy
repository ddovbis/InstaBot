package com.instabot

import com.instabot.config.InstaBotConfig
import com.instabot.operations.liker.relatedusers.RelatedUsersLiker
import com.instabot.operations.reporter.relatedusers.RelatedUsersReporter
import com.instabot.operations.updater.relatedusers.RelatedUsersUpdater
import com.instabot.utils.exceptions.user.UsersLoadingException
import com.instabot.utils.filehandler.PageSourceSaver
import com.instabot.webdriver.InstaWebDriver
import groovy.time.TimeCategory
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

@Component
@DependsOn("initializeInstaBotConfig")
class InstaBot implements CommandLineRunner{
    private static final Logger LOG = LogManager.getLogger(InstaBot.class)

    @Autowired
    private InstaBotConfig initializeInstaBotConfig
    @Autowired
    private InstaWebDriver instaDriver
    @Autowired
    private RelatedUsersUpdater relatedUsersUpdater
    @Autowired
    private RelatedUsersReporter relatedUsersReporter
    @Autowired
    private RelatedUsersLiker relatedUsersLiker

    private String masterUsername
    private int maxIterations

    @Bean("initializeInstaBot")
    @DependsOn("initializeInstaBotConfig")
    private void initialize() {
        LOG.info("Initialize InstaBot")
        masterUsername = initializeInstaBotConfig.getIniFile().get("related-users", "master-username", String.class)
        maxIterations = initializeInstaBotConfig.getIniFile().get("general", "max-iterations", Integer.class)
    }

    @Override
    void run(String... args) throws Exception {
        LOG.info("Spring application successfully initialized")

        Date startTime = new Date()
        try {
            LOG.info("Start InstaBot execution")
            if (masterUsername == instaDriver.primaryUsername) {
                LOG.info("InstaBot has been started in full cycle mode")
                fullCycleMode()
            } else {
                LOG.info("InstaBot has been started in reporting-only mode (master username doesn't belong to the primary user logged into Instagram)")
                startReportingOperations()
            }
        }
        catch (UsersLoadingException e) {
            LOG.error("Could not load all users", e)
            PageSourceSaver.savePageSourceOnException(instaDriver)
            throw e
        } finally {
            LOG.info("All operations are finished; close InstaWebDriver connection...")
            instaDriver.closeConnection()
        }

        LOG.info("Spring application finished; running time: ${TimeCategory.minus(new Date(), startTime)}")
    }

    private void fullCycleMode() {
        startReportingOperations()
        startLoopOperations()
    }

    /**
     * Updates the users related to the master user and reports the results by email
     */
    private void startReportingOperations() {
        // update related users in database
        if (relatedUsersUpdater.updateRelatedUsers()) {
            // send related users report if at least one user has been updated
            relatedUsersReporter.sendReport()
        }
    }

    /**
     * Runs the operations that require pauses between iterations
     * The pauses are required due to the max. nr. of operations of a specific type allowed per hour
     */
    private startLoopOperations() {
        LOG.info("Run loop operations for $maxIterations times")
        for (cycleCounter in (0..<maxIterations)) {
            LOG.info("Start loop operations cycle nr.: $cycleCounter")

            // like posts published by related users
            relatedUsersLiker.likeRelatedUsersPosts()

            // other features here

            LOG.info("Loop operations cycle nr. $cycleCounter is finished; sleep for 1 hour...")
            sleepOneHour()
        }
    }

    /**
     * Sleeps for one hour reporting the progress every minute
     */
    private sleepOneHour() {
        for (sleepMsPassed in (60000..3600000).step(60000)) {
            LOG.debug("Slept minutes: ${sleepMsPassed / 60000} ")
            sleep(60000)
        }
    }


}