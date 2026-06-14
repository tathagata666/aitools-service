package com.costroom.aitoolsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiToolsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiToolsServiceApplication.class, args);
    }
}
