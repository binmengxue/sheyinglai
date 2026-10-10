package com.sheyinglai.storage;

import com.sheyinglai.common.BizException;
import org.springframework.web.multipart.MultipartFile;

import net.coobird.thumbnailator.util.exif.ExifUtils;
import net.coobird.thumbnailator.util.exif.Orientation;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** 图片存储：local（服务器磁盘）或 oss（阿里云），由 app.storage.type 决定 */
public interface StorageService {

    Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "webp", "gif");

    StoredFile store(MultipartFile file) throws IOException;

    void delete(String key);

    /** 生成存储路径：photos/2026/10/uuid.jpg */
    static String newKey(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED.contains(ext)) {
            throw new BizException("只支持 jpg / png / webp / gif 图片");
        }
        LocalDate d = LocalDate.now();
        return String.format("photos/%d/%02d/%s.%s", d.getYear(), d.getMonthValue(),
                UUID.randomUUID().toString().replace("-", ""), ext);
    }

    /**
     * 只读文件头获取宽高（不解码整张图，省内存），并按 EXIF 方向修正竖拍照片。
     * 读不出来（如部分 webp）返回 null。
     */
    static int[] readSize(byte[] bytes) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> it = ImageIO.getImageReaders(in);
            if (!it.hasNext()) return null;
            ImageReader reader = it.next();
            try {
                reader.setInput(in);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                return isRotated(reader) ? new int[]{h, w} : new int[]{w, h};
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            return null;
        }
    }

    /** EXIF 方向 5~8 表示图片需要旋转 90 度显示，宽高要对调 */
    private static boolean isRotated(ImageReader reader) {
        try {
            Orientation o = ExifUtils.getExifOrientation(reader, 0);
            return o != null && ROTATED.contains(o.name());
        } catch (Exception e) {
            return false;
        }
    }

    Set<String> ROTATED = Set.of("LEFT_TOP", "RIGHT_TOP", "RIGHT_BOTTOM", "LEFT_BOTTOM");
}
