package com.plantorder.labor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LaborApplication {

    public static void main(String[] args) {
        SpringApplication.run(LaborApplication.class, args);
    }
}
