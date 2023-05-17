package com.hw.hwbackend;

import com.github.lazyboyl.websocket.integrate.EnableWebSocketServer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

@SpringBootApplication
@MapperScan("com.hw.hwbackend.mapper")
@EnableScheduling
@EnableWebSocketServer(webSocketScanPackage = {"com.hw.hwbackend"})
public class HwBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HwBackendApplication.class, args);
    }

}
