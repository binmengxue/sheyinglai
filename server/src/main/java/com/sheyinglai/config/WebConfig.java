package com.sheyinglai.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties props;

    /** 开启 Sa-Token 注解鉴权（@SaCheckLogin） */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }

    /** local 存储模式下，/api/files/** 直接映射到磁盘目录 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Path.of(props.getStorage().getLocal().getDir()).toAbsolutePath();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("无法创建图片目录 " + dir, e);
        }
        String location = dir.toUri().toString();
        // 资源目录必须以 / 结尾，否则 Spring 找不到文件
        registry.addResourceHandler("/files/**").addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
