package com.huawei.browsergateway.websocket.media.ffmpeg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CodecProcessor 接口测试
 * 验证接口契约的一致性
 */
@DisplayName("CodecProcessor 接口测试")
class CodecProcessorTest {

    @Test
    @DisplayName("验证接口存在")
    void testInterfaceExists() {
        assertTrue(CodecProcessor.class.isInterface(), "CodecProcessor 应该是接口");
    }

    @Test
    @DisplayName("验证 init 方法存在")
    void testInitMethodExists() throws Exception {
        Method method = CodecProcessor.class.getMethod("init",
                com.huawei.browsergateway.websocket.media.MediaParam.class,
                org.bytedeco.ffmpeg.avformat.AVFormatContext.class,
                org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int.class,
                org.bytedeco.javacpp.Pointer.class);

        assertNotNull(method, "init 方法应该存在");
        assertEquals(boolean.class, method.getReturnType(), "init 方法返回类型应该是 boolean");
    }

    @Test
    @DisplayName("验证 getStreamIndex 方法存在")
    void testGetStreamIndexMethodExists() throws Exception {
        Method method = CodecProcessor.class.getMethod("getStreamIndex");

        assertNotNull(method, "getStreamIndex 方法应该存在");
        assertEquals(int.class, method.getReturnType(),
                "getStreamIndex 方法返回类型应该是 int");
        assertEquals(0, method.getParameterCount(), "getStreamIndex 方法不应该有参数");
    }

    @Test
    @DisplayName("验证 streamCodec 方法存在")
    void testStreamCodecMethodExists() throws Exception {
        Method method = CodecProcessor.class.getMethod("streamCodec",
                org.bytedeco.ffmpeg.avcodec.AVPacket.class);

        assertNotNull(method, "streamCodec 方法应该存在");
        assertEquals(void.class, method.getReturnType(),
                "streamCodec 方法返回类型应该是 void");
    }

    @Test
    @DisplayName("验证 close 方法存在")
    void testCloseMethodExists() throws Exception {
        Method method = CodecProcessor.class.getMethod("close");

        assertNotNull(method, "close 方法应该存在");
        assertEquals(void.class, method.getReturnType(), "close 方法返回类型应该是 void");
        assertEquals(0, method.getParameterCount(), "close 方法不应该有参数");
    }

    @Test
    @DisplayName("验证接口方法数量")
    void testInterfaceMethodCount() {
        Method[] methods = CodecProcessor.class.getDeclaredMethods();
        assertEquals(4, methods.length, "CodecProcessor 接口应该有 4 个方法");
    }
}
