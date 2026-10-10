package com.sheyinglai.storage;

import com.sheyinglai.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** 图片存到服务器磁盘，适合起步阶段；图片多了建议切换到 OSS */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path root;
    private final String urlPrefix;
    private final int thumbWidth;

    public LocalStorageService(AppProperties props) {
        this.root = Path.of(props.getStorage().getLocal().getDir()).toAbsolutePath();
        this.urlPrefix = props.getStorage().getLocal().getUrlPrefix();
        this.thumbWidth = props.getStorage().getThumbWidth();
    }

    @Override
    public StoredFile store(MultipartFile file) throws IOException {
        String key = StorageService.newKey(file);
        byte[] bytes = file.getBytes();
        Path target = root.resolve(key);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);

        String url = urlPrefix + key;
        String thumbUrl = url;
        String thumbKey = thumbKey(key);
        try {
            Thumbnails.of(new ByteArrayInputStream(bytes))
                    .width(thumbWidth).addFilter(LocalStorageService::onWhite).outputFormat("jpg").outputQuality(0.8)
                    .toFile(root.resolve(thumbKey).toFile());
            thumbUrl = urlPrefix + thumbKey;
        } catch (Exception e) {
            log.warn("缩略图生成失败，使用原图：{}", key);
        }

        int[] size = StorageService.readSize(bytes);
        return new StoredFile(key, url, thumbUrl,
                size == null ? null : size[0], size == null ? null : size[1], bytes.length);
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(root.resolve(key));
            Files.deleteIfExists(root.resolve(thumbKey(key)));
        } catch (IOException e) {
            log.warn("删除文件失败：{}", key, e);
        }
    }

    /** 透明 PNG/GIF 转 JPG 时铺白底，否则透明处会变黑 */
    private static BufferedImage onWhite(BufferedImage img) {
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return out;
    }

    /** photos/2026/10/abc.png -> photos/2026/10/abc_thumb.jpg */
    private static String thumbKey(String key) {
        return key.substring(0, key.lastIndexOf('.')) + "_thumb.jpg";
    }
}
