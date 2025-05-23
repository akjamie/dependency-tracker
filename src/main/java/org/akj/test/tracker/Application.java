package org.akj.test.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "org.akj.test.tracker.infrastructure.storage")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
