package com.instabot

import groovy.time.TimeCategory
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.stereotype.Component

@Component
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.instabot.data.repositories.user")
class Main implements CommandLineRunner {
    public static final Logger LOG = LogManager.getLogger(Main.class)

    @Autowired
    InstaBot instaBot

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(Main.class, args)
    }

    @Override
    void run(String... args) throws Exception {
        LOG.info("Spring application successfully initialized")
        Date startTime = new Date()
        String loginUsername = args[0]
        String loginPassword = args[1]

        instaBot.start(loginUsername, loginPassword)
        LOG.info "Spring application finished; running time: ${TimeCategory.minus(new Date(), startTime)}"
    }
}
