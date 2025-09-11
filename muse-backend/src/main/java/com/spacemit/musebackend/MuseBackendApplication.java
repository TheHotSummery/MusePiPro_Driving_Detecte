package com.spacemit.musebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MuseBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MuseBackendApplication.class, args);
    }

}