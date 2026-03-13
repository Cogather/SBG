package com.huawei.browsergateway.websocket.media.ffmpeg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CodecProcessor 接口测试
 * 验证接口契约的一致性
 */
@DisplayName("CodecProcessor interface tests")
class CodecProcessorTest {

    @Test
    @DisplayName("Interface exists")
    void testInterfaceExists() {
        assertTrue(CodecProcessor.class.isInterface(), "CodecProcessor 应该是接口");
    }

    @Test
    @DisplayName("init method exists")
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
    @DisplayName("getStreamIndex method exists")
    void testGetStreamIndexMethodExists() throws Exception {
        Method method = CodecProcessor.class.getMethod("getStreamIndex");

        assertNotNull(method, "getStreamIndex 方法应该存在");
        assertEquals(int.class, method.getReturnType(),
                "getStreamIndex 方法返回类型应该是 int");
        assertEquals(0, method.getParameterCount(), "getStreamIndex 方法不应该有参数");
    }

    @Test
    @DisplayName("streamCodec method exists")
    void testStreamCodecMethodExists() throws Exception {
        Method method = CodecProcessor.class.getMethod("streamCodec",
                org.bytedeco.ffmpeg.avcodec.AVPacket.class);

        assertNotNull(method, "streamCodec 方法应该存在");
        assertEquals(void.class, method.getReturnType(),
                "streamCodec 方法返回类型应该是 void");
    }

    @Test
    @DisplayName("close method exists")
    void testCloseMethodExists() throws Exception {
        Method method = CodecProcessor.class.getMethod("close");

        assertNotNull(method, "close 方法应该存在");
        assertEquals(void.class, method.getReturnType(), "close 方法返回类型应该是 void");
        assertEquals(0, method.getParameterCount(), "close 方法不应该有参数");
    }

    @Test
    @DisplayName("Interface method count")
    void testInterfaceMethodCount() {
        Method[] methods = CodecProcessor.class.getDeclaredMethods();
        assertEquals(4, methods.length, "CodecProcessor 接口应该有 4 个方法");
    }
}
