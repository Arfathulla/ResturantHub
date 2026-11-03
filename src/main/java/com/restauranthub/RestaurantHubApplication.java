package com.restauranthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class RestaurantHubApplication {

    public static void main(String[] args) {
        // Enforce system default timezone to UTC for predictable timestamp calculations
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(RestaurantHubApplication.class, args);
    }
}
