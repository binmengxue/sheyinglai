package com.sheyinglai;

import com.fasterxml.jackson.databind.JsonNode;
import com.sheyinglai.common.BizException;
import com.sheyinglai.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

class SecurityTest extends ApiTestSupport {

    @Test
    void weakPasswordRejected() throws Exception {
        JsonNode r = call("POST", "/auth/register",
                Map.of("phone", "13900000001", "password", "123456", "agree", true), null);
        assertThat(r.get("code").asInt()).isEqualTo(400);
        assertThat(r.get("msg").asText()).contains("字母和数字");
    }

    @Test
    void loginIsRateLimitedPerPhone() throws Exception {
        int last = 0;
        for (int i = 0; i < 11; i++) {
            last = call("POST", "/auth/login", Map.of("phone", "13900000002", "password", "wrong123"), null)
                    .get("code").asInt();
        }
        assertThat(last).isEqualTo(429);
    }

    @Test
    void registerIsRateLimitedPerIp() throws Exception {
        int last = 0;
        for (int i = 0; i < 6; i++) {
            byte[] body = json.writeValueAsBytes(Map.of("phone", "1370000000" + i, "password", "test1234", "agree", true));
            String text = mvc.perform(request(HttpMethod.POST, "/auth/register").header("X-Real-IP", "192.168.9.9")
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            last = json.readTree(text).get("code").asInt();
        }
        assertThat(last).isEqualTo(429);
    }

    @Test
    void nonImageUploadRejected() {
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> StorageService.inspect(html)).isInstanceOf(BizException.class);
    }

    @Test
    void realImageDetectedByContentNotName() throws Exception {
        StorageService.ImageInfo info = StorageService.inspect(png(20, 10));
        assertThat(info.ext()).isEqualTo("png");
        assertThat(info.contentType()).isEqualTo("image/png");
        assertThat(info.width()).isEqualTo(20);
    }

    @Test
    void hugePixelImageRejectedBeforeDecoding() throws Exception {
        byte[] bytes = png(10, 10);
        // 把 IHDR 里的宽高改成 30000×30000：文件仍然很小，但解码会占用约 3.6GB 内存
        ByteBuffer.wrap(bytes, 16, 8).putInt(30000).putInt(30000);
        CRC32 crc = new CRC32();
        crc.update(bytes, 12, 17);
        ByteBuffer.wrap(bytes, 29, 4).putInt((int) crc.getValue());
        assertThatThrownBy(() -> StorageService.inspect(bytes)).isInstanceOf(BizException.class)
                .hasMessageContaining("尺寸过大");
    }

    private static byte[] png(int w, int h) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }
}
