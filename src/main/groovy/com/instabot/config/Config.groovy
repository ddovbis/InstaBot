package com.instabot.config

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Config {
    private static final Logger LOG = LogManager.getLogger(Config.class)

    @Bean("loadSystemProperties")
    protected loadSystemProperties() {
        File systemProperties = new File("./src/main/resources/system.properties")

        LOG.info("Load properties from ${systemProperties.getAbsolutePath()}")
        Properties newProperties = new Properties()
        newProperties.load(new StringReader(systemProperties.text))
        LOG.info("Loaded ${newProperties.size()} properties")

        LOG.info("Set system properties based on loaded properties")
        newProperties.each { property, value ->
            LOG.info("Set system property $property: $value")
            System.setProperty(property as String, value as String)
        }
    }
}
