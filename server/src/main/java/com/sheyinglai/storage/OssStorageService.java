package com.sheyinglai.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.sheyinglai.config.AppProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 图片存到阿里云 OSS。缩略图不另存文件，直接用 OSS 图片处理参数实时生成。
 * 需要在 OSS 控制台把 Bucket 设为「公共读」，或绑定 CDN 域名。
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "oss")
public class OssStorageService implements StorageService {

    private final OSS client;
    private final AppProperties.Oss cfg;
    private final int thumbWidth;

    public OssStorageService(AppProperties props) {
        this.cfg = props.getStorage().getOss();
        this.thumbWidth = props.getStorage().getThumbWidth();
        this.client = new OSSClientBuilder().build(cfg.getEndpoint(), cfg.getAccessKeyId(), cfg.getAccessKeySecret());
    }

    @Override
    public StoredFile store(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        ImageInfo info = StorageService.inspect(bytes);
        String key = StorageService.newKey(info.ext());
        ObjectMetadata meta = new ObjectMetadata();
        // 按识别出的格式设置，不信任客户端传来的 Content-Type
        meta.setContentType(info.contentType());
        meta.setContentLength(bytes.length);
        client.putObject(cfg.getBucket(), key, new ByteArrayInputStream(bytes), meta);

        String url = trimSlash(cfg.getDomain()) + "/" + key;
        String thumbUrl = url + "?x-oss-process=image/resize,w_" + thumbWidth + "/quality,q_80/format,jpg";
        return new StoredFile(key, url, thumbUrl, info.width(), info.height(), bytes.length);
    }

    @Override
    public void delete(String key) {
        client.deleteObject(cfg.getBucket(), key);
    }

    @PreDestroy
    public void shutdown() {
        client.shutdown();
    }

    private static String trimSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
