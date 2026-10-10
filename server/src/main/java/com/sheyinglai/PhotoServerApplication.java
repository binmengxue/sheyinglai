package com.sheyinglai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.sheyinglai.mapper")
public class PhotoServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PhotoServerApplication.class, args);
    }
}
