package com.plantorder.cost;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CostApplication {

    public static void main(String[] args) {
        SpringApplication.run(CostApplication.class, args);
    }
}
