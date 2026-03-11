package com.huawei.browsergateway.websocket.media.ffmpeg;

import com.huawei.browsergateway.websocket.media.MediaParam;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int;
import org.bytedeco.javacpp.Pointer;

/**
 * 编解码处理器接口
 * 定义音视频编解码的通用操作
 */
public interface CodecProcessor {

    /**
     * 初始化编解码器
     *
     * @param mediaParam   媒体参数
     * @param inFormatCtx  输入格式上下文
     * @param writePacket  写数据包回调
     * @param userIdPtr    用户ID指针
     * @return 初始化是否成功
     */
    boolean init(MediaParam mediaParam, AVFormatContext inFormatCtx,
                 Write_packet_Pointer_BytePointer_int writePacket, Pointer userIdPtr);

    /**
     * 获取流索引
     *
     * @return 流索引
     */
    int getStreamIndex();

    /**
     * 处理流编解码
     *
     * @param pkt 数据包
     */
    void streamCodec(AVPacket pkt);

    /**
     * 关闭编解码器并释放资源
     */
    void close();
}
