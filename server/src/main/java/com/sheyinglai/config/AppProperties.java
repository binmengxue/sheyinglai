package com.sheyinglai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 中 app.* 配置 */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Admin admin = new Admin();
    private Storage storage = new Storage();

    @Data
    public static class Admin {
        /** 平台管理员，启动时由 AdminInitializer 按这里的手机号和密码创建或同步 */
        private String phone = "13800000000";
        private String password = "admin123";
    }

    @Data
    public static class Storage {
        /** local：存服务器磁盘；oss：存阿里云 OSS */
        private String type = "local";
        /** 缩略图宽度（像素） */
        private int thumbWidth = 600;
        private Local local = new Local();
        private Oss oss = new Oss();
    }

    @Data
    public static class Local {
        /** 图片保存目录 */
        private String dir = "./uploads";
        /** 访问前缀，对应 WebConfig 中的静态资源映射 */
        private String urlPrefix = "/api/files/";
    }

    @Data
    public static class Oss {
        /** 如 https://oss-cn-shanghai.aliyuncs.com（服务器同地域可用 -internal 内网地址，免流量费） */
        private String endpoint;
        private String bucket;
        private String accessKeyId;
        private String accessKeySecret;
        /** 图片访问域名：绑定的 CDN 域名或 bucket 外网域名，如 https://img.sheyinglai.top */
        private String domain;
    }
}
