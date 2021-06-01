package com.instabot

import com.instabot.utils.argshandler.ArgsHandler
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
@EnableJpaRepositories(basePackages = "com.instabot.data.repositories")
class Main  {
    static void main(String[] args) throws InterruptedException {
        ArgsHandler.process(args)
        SpringApplication.run(Main.class, args)
    }
}
