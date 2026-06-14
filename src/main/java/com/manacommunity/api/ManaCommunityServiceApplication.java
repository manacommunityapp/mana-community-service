package com.manacommunity.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling activates @Scheduled jobs (notification dispatch, expiry
// cleanup, and the new per-match email reminders). It was previously absent,
// so those jobs never fired.
@EnableScheduling
@SpringBootApplication
public class ManaCommunityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManaCommunityServiceApplication.class, args);
    }

}
