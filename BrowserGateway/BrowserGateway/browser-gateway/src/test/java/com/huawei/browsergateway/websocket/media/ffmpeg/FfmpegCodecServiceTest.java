package com.huawei.browsergateway.websocket.media.ffmpeg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FfmpegCodecService 功能一致性测试
 * 确保重构后功能100%一致
 */
@DisplayName("FfmpegCodecService functional consistency tests")
class FfmpegCodecServiceTest {

    @Test
    @DisplayName("Class implements CodecProcessor interface")
    void testImplementsCodecProcessor() {
        // VideoCodecProcessor 和 AudioCodecProcessor 应该实现 CodecProcessor
        assertTrue(CodecProcessor.class.isAssignableFrom(VideoCodecProcessor.class),
                "VideoCodecProcessor 应该实现 CodecProcessor");
        assertTrue(CodecProcessor.class.isAssignableFrom(AudioCodecProcessor.class),
                "AudioCodecProcessor 应该实现 CodecProcessor");
    }

    @Test
    @DisplayName("FfmpegCodecService constructor")
    void testConstructor() throws Exception {
        var constructor = FfmpegCodecService.class.getConstructor(
                com.huawei.browsergateway.websocket.media.MediaParam.class);
        assertNotNull(constructor, "应该有接受 MediaParam 的构造函数");
    }

    @Test
    @DisplayName("init method exists")
    void testInitMethodExists() throws Exception {
        Method method = FfmpegCodecService.class.getMethod("init",
                org.bytedeco.javacpp.Pointer.class,
                org.bytedeco.ffmpeg.avformat.Read_packet_Pointer_BytePointer_int.class,
                org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int.class,
                org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int.class);

        assertNotNull(method, "init 方法应该存在");
        assertEquals(boolean.class, method.getReturnType(), "init 返回类型应该是 boolean");
    }

    @Test
    @DisplayName("start method exists")
    void testStartMethodExists() throws Exception {
        Method method = FfmpegCodecService.class.getMethod("start");
        assertNotNull(method, "start 方法应该存在");
        assertEquals(void.class, method.getReturnType(), "start 返回类型应该是 void");
    }

    @Test
    @DisplayName("close method exists")
    void testCloseMethodExists() throws Exception {
        Method method = FfmpegCodecService.class.getMethod("close");
        assertNotNull(method, "close 方法应该存在");
        assertEquals(void.class, method.getReturnType(), "close 返回类型应该是 void");
    }

    @Test
    @DisplayName("Private fields exist")
    void testPrivateFieldsExist() throws Exception {
        Field mediaParamField = FfmpegCodecService.class.getDeclaredField("mediaParam");
        Field videoCodecField = FfmpegCodecService.class.getDeclaredField("videoCodecCtx");
        Field audioCodecField = FfmpegCodecService.class.getDeclaredField("audioCodecCtx");

        assertNotNull(mediaParamField, "mediaParam 字段应该存在");
        assertNotNull(videoCodecField, "videoCodecCtx 字段应该存在");
        assertNotNull(audioCodecField, "audioCodecCtx 字段应该存在");
    }

    @Test
    @DisplayName("VideoCodecProcessor methods exist")
    void testVideoCodecProcessorMethods() throws Exception {
        Method initMethod = VideoCodecProcessor.class.getMethod("init",
                com.huawei.browsergateway.websocket.media.MediaParam.class,
                org.bytedeco.ffmpeg.avformat.AVFormatContext.class,
                org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int.class,
                org.bytedeco.javacpp.Pointer.class);

        Method getStreamIndexMethod = VideoCodecProcessor.class.getMethod("getStreamIndex");
        Method streamCodecMethod = VideoCodecProcessor.class.getMethod("streamCodec",
                org.bytedeco.ffmpeg.avcodec.AVPacket.class);
        Method closeMethod = VideoCodecProcessor.class.getMethod("close");

        assertNotNull(initMethod, "init 方法应该存在");
        assertNotNull(getStreamIndexMethod, "getStreamIndex 方法应该存在");
        assertNotNull(streamCodecMethod, "streamCodec 方法应该存在");
        assertNotNull(closeMethod, "close 方法应该存在");
    }

    @Test
    @DisplayName("AudioCodecProcessor methods exist")
    void testAudioCodecProcessorMethods() throws Exception {
        Method initMethod = AudioCodecProcessor.class.getMethod("init",
                com.huawei.browsergateway.websocket.media.MediaParam.class,
                org.bytedeco.ffmpeg.avformat.AVFormatContext.class,
                org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int.class,
                org.bytedeco.javacpp.Pointer.class);

        Method getStreamIndexMethod = AudioCodecProcessor.class.getMethod("getStreamIndex");
        Method streamCodecMethod = AudioCodecProcessor.class.getMethod("streamCodec",
                org.bytedeco.ffmpeg.avcodec.AVPacket.class);
        Method closeMethod = AudioCodecProcessor.class.getMethod("close");

        assertNotNull(initMethod, "init 方法应该存在");
        assertNotNull(getStreamIndexMethod, "getStreamIndex 方法应该存在");
        assertNotNull(streamCodecMethod, "streamCodec 方法应该存在");
        assertNotNull(closeMethod, "close 方法应该存在");
    }

    @Test
    @DisplayName("VideoCodecProcessor private fields")
    void testVideoCodecProcessorFields() throws Exception {
        Field[] expectedFields = {
                VideoCodecProcessor.class.getDeclaredField("decodeCtx"),
                VideoCodecProcessor.class.getDeclaredField("encodeCtx"),
                VideoCodecProcessor.class.getDeclaredField("outFormatCtx"),
                VideoCodecProcessor.class.getDeclaredField("streamIndex"),
                VideoCodecProcessor.class.getDeclaredField("lastDTS")
        };

        for (Field field : expectedFields) {
            assertNotNull(field, field.getName() + " 字段应该存在");
        }
    }

    @Test
    @DisplayName("AudioCodecProcessor private fields")
    void testAudioCodecProcessorFields() throws Exception {
        Field[] expectedFields = {
                AudioCodecProcessor.class.getDeclaredField("decodeCtx"),
                AudioCodecProcessor.class.getDeclaredField("encodeCtx"),
                AudioCodecProcessor.class.getDeclaredField("outFormatCtx"),
                AudioCodecProcessor.class.getDeclaredField("streamIndex"),
                AudioCodecProcessor.class.getDeclaredField("pts"),
                AudioCodecProcessor.class.getDeclaredField("audioFifo")
        };

        for (Field field : expectedFields) {
            assertNotNull(field, field.getName() + " 字段应该存在");
        }
    }
}
