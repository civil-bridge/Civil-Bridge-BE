package org.example.civilbridge;

import org.example.civilbridge.common.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(JwtProperties.class)
public class CivilBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CivilBridgeApplication.class, args);
    }

}
