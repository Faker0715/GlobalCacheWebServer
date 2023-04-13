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
//        System.out.println(TimeZone.getDefault().getID());
//        System.out.println(System.currentTimeMillis());
//        LocalDateTime now = LocalDateTime.now();
// 获取指定时区的ZoneId对象
//        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
// 将LocalDateTime对象和ZoneId对象组合成ZonedDateTime对象
//        ZonedDateTime zonedDateTime = ZonedDateTime.of(now, zoneId);
//        System.out.println(zonedDateTime.getHour());
//        System.out.println(zonedDateTime.getMinute());
//        System.out.println(zonedDateTime.getSecond());
        SpringApplication.run(HwBackendApplication.class, args);
    }

}
