package com.sheyinglai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.sheyinglai.mapper")
@EnableScheduling
public class PhotoServerApplication {
    /** 业务全部按北京时间算（「今天」、可约时段、每日统计），不依赖服务器的时区设置 */
    public static final String ZONE = "Asia/Shanghai";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONE));
        SpringApplication.run(PhotoServerApplication.class, args);
    }
}
