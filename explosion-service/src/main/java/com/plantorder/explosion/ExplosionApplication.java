package com.plantorder.explosion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExplosionApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExplosionApplication.class, args);
    }
}
