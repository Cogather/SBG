package com.huawei.browsergateway.websocket.media.ffmpeg;

import com.huawei.browsergateway.websocket.media.MediaParam;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.Write_packet_Pointer_BytePointer_int;
import org.bytedeco.javacpp.Pointer;

public interface CodecProcessor {
    boolean init(MediaParam mediaParam, AVFormatContext inFormatCtx, Write_packet_Pointer_BytePointer_int writePacket
            , Pointer userIdPtr);
    int getStreamIndex();
    void streamCodec(AVPacket pkt) ;
    void close();
}