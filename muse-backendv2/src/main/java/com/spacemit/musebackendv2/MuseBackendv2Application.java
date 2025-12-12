package com.spacemit.musebackendv2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync  // 启用异步支持（用于逆地理编码）
@EnableScheduling  // 启用定时任务支持（用于行程更新）
public class MuseBackendv2Application {

    public static void main(String[] args) {
        SpringApplication.run(MuseBackendv2Application.class, args);
    }

}
