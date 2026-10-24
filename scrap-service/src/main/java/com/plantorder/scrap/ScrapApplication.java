package com.plantorder.scrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScrapApplication.class, args);
    }
}
