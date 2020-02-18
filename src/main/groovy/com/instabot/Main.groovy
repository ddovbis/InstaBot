package com.instabot

import com.instabot.config.Config
import com.instabot.data.model.user.User
import com.instabot.data.repository.user.UserRepository
import com.instabot.tmptest.Customer
import com.instabot.tmptest.CustomerRepository
import org.apache.log4j.Logger
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


import java.awt.*

@SpringBootApplication
@ImportResource("classpath:context.xml")
class Main {
    public static final Logger LOG = Logger.getLogger(Main.class)

    static UserRepository testRepository

    public static void main(String[] args) throws InterruptedException, AWTException {
        // set up configuration based on default setup and InstaBot.ini file
        Config.init()
        LOG.info("Start InstaBot application")
        SpringApplication.run(Main.class, args);

        User user = new User("master_username_ex2", "username_ex2", "name_ex2")
        testRepository.save(user)


//userRepository

//
//        // start process
//        new InstaBot().start()
        LOG.info "InstaBot execution finished"
    }

    @Bean
    public test(UserRepository repository) {
        testRepository = repository
    }

    @Bean
    public CommandLineRunner demo(CustomerRepository repository) {
        return (args) -> {
            // save a few customers
            repository.save(new Customer("Jack", "Bauer"));
            repository.save(new Customer("Chloe", "O'Brian"));
            repository.save(new Customer("Kim", "Bauer"));
            repository.save(new Customer("David", "Palmer"));
            repository.save(new Customer("Michelle", "Dessler1"));
            repository.save(new Customer("Michelle2", "Dessler3"));

            // fetch all customers
            LOG.info("Customers found with findAll():");
            LOG.info("-------------------------------");
            for (Customer customer : repository.findAll()) {
                LOG.info(customer.toString());
            }
            LOG.info("");

            // fetch an individual customer by ID
            Customer customer = repository.findById(1L);
            LOG.info("Customer found with findById(1L):");
            LOG.info("--------------------------------");
            LOG.info(customer.toString());
            LOG.info("");

            // fetch customers by last name
            LOG.info("Customer found with findByLastName('Bauer'):");
            LOG.info("--------------------------------------------");
            repository.findByLastName("Bauer").forEach(bauer -> {
                LOG.info(bauer.toString());
            });
            // for (Customer bauer : repository.findByLastName("Bauer")) {
            //  LOG.info(bauer.toString());
            // }
            LOG.info("");

//            sleep(25000)
        };
    }
}
