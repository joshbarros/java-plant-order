package com.plantorder.bom;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BomApplication {

    public static void main(String[] args) {
        SpringApplication.run(BomApplication.class, args);
    }
}
