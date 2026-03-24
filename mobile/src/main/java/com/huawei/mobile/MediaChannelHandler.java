package com.huawei.mobile;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.huawei.mobile.common.ID;
import com.huawei.mobile.common.Type;
import com.huawei.mobile.encode.TlbData;
import com.huawei.mobile.encode.TlvData;
import com.huawei.mobile.encode.TlvDecoder;
import com.huawei.mobile.encode.TlvEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MediaChannelHandler  extends ChannelInboundHandlerAdapter {
    private static final Log log = LogFactory.get(MediaChannelHandler.class);

    private final BrowserContext demo;

    public MediaChannelHandler(BrowserContext demo) {
        this.demo = demo;
    }


    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException, IOException, IOException {
        TlbData data = (TlbData) msg;
        int type = data.get(ID.TYPE).readInt();
        switch (type) {
            case Type.ACK:
                int ackType = data.get(ID.ACK_TYPE).readInt();
                int code = data.get(ID.CODE).readInt();
                log.info("receive ack :{}, code is: {}", ackType, code);
                if (code != 200) {
                    ctx.close().sync();
                }
                if (ackType == Type.LOGIN) {
                    ThreadUtil.execute(() -> {
                        while (ctx.channel().isActive()) {
                            try {
                                TlvData<Object> tlvData = new TlvData<>();
                                tlvData.put(ID.TYPE, Type.HEARTBEATS);
                                ctx.channel().writeAndFlush(tlvData);
                                ThreadUtil.sleep(30, TimeUnit.SECONDS);
                            }catch (Exception e) {
                                log.error(e);
                                break;
                            }
                        }
                    });
                }
                break;
            case Type.AUDIO:
                ByteBuf audioBuf = data.get(ID.AUDIO_DATA);
                if (audioBuf != null) {
                    int length = audioBuf.readableBytes();
                    byte[] bytes = new byte[length + 1];
                    bytes[0] = 2;
                    // 从当前readerIndex开始复制，不改变指针位置
                    audioBuf.getBytes(audioBuf.readerIndex(), bytes, 1, length);
//                    log.info("callbackAudio: {}", bytes.length);
                    this.demo.callbackVideo(bytes);
                }
                break;
            case Type.VIDEO:
                ByteBuf byteBuf = data.get(ID.VIDEO_DATA);
                if (byteBuf != null) {
                    ByteBuf byteBuf1 = data.get(ID.FRAME_TYPE);
                    int frameType = byteBuf1.readInt();

                    int length = byteBuf.readableBytes();
                    byte[] bytes = new byte[length + 2];
                    bytes[0] = 1;
                    bytes[1] = (byte) frameType;
                    // 从当前readerIndex开始复制，不改变指针位置
                    byteBuf.getBytes(byteBuf.readerIndex(), bytes, 2, length);
                    // 使用字节数组...
//                    log.info("callbackVideo: {}", bytes.length);
                    this.demo.callbackVideo(bytes);
                }
                break;
            default:
                break;
        }
        data.releaseAll();
    }
}