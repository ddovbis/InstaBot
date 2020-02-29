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

    //TODO Not needed
    @Autowired
    private Environment environment

    @Bean
    protected loadSystemProperties() {
        // TODO Don't use an absolute path here!!! Try to read system.properties as a resource on the classpath.
        // This way, you can package you app as a jar, but still put system.properties on the classpath and it will get loaded.
        File systemProperties = new File("./src/main/resources/system.properties")
        int systemPropertiesInitialAmount = System.properties.size()

        LOG.info("Load system properties from ${systemProperties.getAbsolutePath()}")
        System.properties.load(new StringReader(systemProperties.text))
        // TODO Log each property that was loaded. It going to be a lot more useful.
        LOG.info("Successfully loaded ${System.properties.size() - systemPropertiesInitialAmount} new system properties")
    }
}
