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
import java.util.Set;
import java.util.UUID;

/** 图片存储：local（服务器磁盘）或 oss（阿里云），由 app.storage.type 决定 */
public interface StorageService {

    /** 像素上限：解码时内存约为 宽×高×4 字节，4000 万像素约 160MB */
    long MAX_PIXELS = 40_000_000L;
    int MAX_SIDE = 12_000;

    StoredFile store(MultipartFile file) throws IOException;

    void delete(String key);

    /** 上传图片的真实格式和尺寸；width/height 为空表示读不出尺寸（部分 webp），此时不做解码 */
    record ImageInfo(String ext, String contentType, Integer width, Integer height) {
        public boolean decodable() {
            return width != null;
        }
    }

    /**
     * 按文件内容（而不是扩展名和客户端传的 Content-Type）判断图片格式，并限制像素尺寸，
     * 防止伪装成图片的文件被当成网页打开，以及超大像素的图片在解码时撑爆内存。
     */
    static ImageInfo inspect(byte[] bytes) {
        String ext = sniff(bytes);
        if (ext == null) {
            throw new BizException("文件不是有效的图片，只支持 jpg / png / webp / gif");
        }
        int[] size = readSize(bytes);
        if (size == null) {
            if (!"webp".equals(ext)) throw new BizException("图片文件已损坏，请换一张");
            return new ImageInfo(ext, "image/webp", null, null);
        }
        if (size[0] > MAX_SIDE || size[1] > MAX_SIDE || (long) size[0] * size[1] > MAX_PIXELS) {
            throw new BizException("图片尺寸过大，请压缩后再上传");
        }
        return new ImageInfo(ext, "image/" + ("jpg".equals(ext) ? "jpeg" : ext), size[0], size[1]);
    }

    /** 看文件头的魔数 */
    private static String sniff(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return "gif";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }

    /** 生成存储路径：photos/2026/10/uuid.jpg，扩展名用识别出的真实格式 */
    static String newKey(String ext) {
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
