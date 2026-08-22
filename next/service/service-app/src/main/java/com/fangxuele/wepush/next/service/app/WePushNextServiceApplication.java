package com.fangxuele.wepush.next.service.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WePushNextServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WePushNextServiceApplication.class, args);
    }
}
