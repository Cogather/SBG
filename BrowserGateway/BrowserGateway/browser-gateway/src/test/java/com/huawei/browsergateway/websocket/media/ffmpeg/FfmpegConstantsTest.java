package com.huawei.browsergateway.websocket.media.ffmpeg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FfmpegConstants 测试类
 * 确保常量定义的一致性
 */
@DisplayName("FfmpegConstants 测试")
class FfmpegConstantsTest {

    @Test
    @DisplayName("验证 BUFFER_SIZE 常量")
    void testBufferSize() {
        assertEquals(65536, FfmpegConstants.BUFFER_SIZE, "BUFFER_SIZE 应该为 65536");
    }

    @Test
    @DisplayName("验证 CACHE_SIZE 常量")
    void testCacheSize() {
        assertEquals(FfmpegConstants.BUFFER_SIZE * 10, FfmpegConstants.CACHE_SIZE,
                "CACHE_SIZE 应该是 BUFFER_SIZE 的 10 倍");
    }

    @Test
    @DisplayName("验证 CONTAIN_FORMAT 常量")
    void testContainFormat() {
        assertEquals("webm", FfmpegConstants.CONTAIN_FORMAT, "CONTAIN_FORMAT 应该为 'webm'");
    }

    @Test
    @DisplayName("验证 VIDEO_ENCODE_FORMAT 常量")
    void testVideoEncodeFormat() {
        assertEquals("h264", FfmpegConstants.VIDEO_ENCODE_FORMAT,
                "VIDEO_ENCODE_FORMAT 应该为 'h264'");
    }

    @Test
    @DisplayName("验证 AUDIO_ENCODE_FORMAT 常量")
    void testAudioEncodeFormat() {
        assertEquals("mp3", FfmpegConstants.AUDIO_ENCODE_FORMAT,
                "AUDIO_ENCODE_FORMAT 应该为 'mp3'");
    }

    @Test
    @DisplayName("验证 AV_FMT_FLAG_FLUSH_PACKETS 常量")
    void testAvFmtFlagFlushPackets() {
        assertEquals(0x80, FfmpegConstants.AV_FMT_FLAG_FLUSH_PACKETS,
                "AV_FMT_FLAG_FLUSH_PACKETS 应该为 0x80");
    }

    @Test
    @DisplayName("验证 AV_PROFILE_H264_BASELINE 常量")
    void testAvProfileH264Baseline() {
        assertEquals(66, FfmpegConstants.AV_PROFILE_H264_BASELINE,
                "AV_PROFILE_H264_BASELINE 应该为 66");
    }

    @Test
    @DisplayName("验证所有常量都是 public static final")
    void testConstantsModifiers() throws Exception {
        Field[] fields = FfmpegConstants.class.getDeclaredFields();
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            assertTrue(Modifier.isPublic(modifiers), field.getName() + " 应该是 public");
            assertTrue(Modifier.isStatic(modifiers), field.getName() + " 应该是 static");
            assertTrue(Modifier.isFinal(modifiers), field.getName() + " 应该是 final");
        }
    }

    @Test
    @DisplayName("验证类是 final 的")
    void testClassIsFinal() {
        assertTrue(Modifier.isFinal(FfmpegConstants.class.getModifiers()),
                "FfmpegConstants 类应该是 final");
    }
}
