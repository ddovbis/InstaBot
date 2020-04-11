package com.instabot.config

import org.apache.logging.log4j.LogManager
import org.ini4j.Wini
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Component

@Component
class InstaBotConfig {
    private static final LOG = LogManager.getLogger(InstaBotConfig.class)

    Wini iniFile

    @Bean("initializeInstaBotConfig")
    @DependsOn("loadSystemProperties")
    protected initialize() {
        LOG.info("Initialize InstaBot configuration file")
        String configurationFilePath = System.getProperty("instabot.conf.path")
        File configurationFile = new File(configurationFilePath)
        if (configurationFile == null) {
            throw new FileNotFoundException("Could not find insta-bot configuration file by path: $configurationFilePath")
        }
        iniFile = new Wini(configurationFile)
        LOG.info("InstaBot configuration file successfully initialized")
    }
}
