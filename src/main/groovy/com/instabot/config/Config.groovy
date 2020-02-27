package com.instabot.config

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class Config {
    private static final Logger LOG = LogManager.getLogger(Config.class)

    @Autowired
    private Environment environment

    @Bean
    protected loadSystemProperties() {
        File systemProperties = new File("./src/main/resources/system.properties")
        int systemPropertiesInitialAmount = System.properties.size()

        LOG.info("Load system properties from ${systemProperties.getAbsolutePath()}")
        System.properties.load(new StringReader(systemProperties.text))
        LOG.info("Successfully loaded ${System.properties.size() - systemPropertiesInitialAmount} new system properties")
    }
}
