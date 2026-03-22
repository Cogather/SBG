package com.huawei.mobile;

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

public class MediaChannelHandler {
    private static final Log log = LogFactory.get();
    private BrowserContext browserContext;
    private Channel channel;
    private final EventLoopGroup group = new NioEventLoopGroup();

    public MediaChannelHandler(BrowserContext browserContext) {
        this.browserContext = browserContext;
    }

    public void connect(String ip, int port) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new TlvDecoder());
                        ch.pipeline().addLast(new TlvEncoder());
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(ChannelHandlerContext ctx) {
                                loginToMedia(ctx.channel());
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                TlbData data = (TlbData) msg;
                                try {
                                    Object typeObj = data.get(ID.TYPE);
                                    if (typeObj == null) return;
                                    int type;
                                    if (typeObj instanceof Integer) {
                                        type = (Integer) typeObj;
                                    } else {
                                        type = Integer.parseInt(typeObj.toString());
                                    }
                                    if (type == Type.AUDIO) {
                                        handleAudioData(data);
                                    } else if (type == Type.VIDEO) {
                                        handleVideoData(data);
                                    }
                                } finally {
                                    data.clear();
                                }
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                log.error("media channel error", cause);
                                ctx.close();
                            }
                        });
                    }
                });
        ChannelFuture future = bootstrap.connect(ip, port);
        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                channel = f.channel();
                log.info("media channel connected {}:{}", ip, port);
            } else {
                log.error("media channel connect fail {}:{}", ip, port);
            }
        });
    }

    private void loginToMedia(Channel ch) {
        TlvData<Object> data = new TlvData<>();
        data.put(ID.TYPE, Type.LOGIN);
        data.put(ID.FACTORY, browserContext.getRequest().getManufacturer());
        data.put(ID.DEV_TYPE, browserContext.getRequest().getModel());
        data.put(ID.IMSI, browserContext.getRequest().getImsi());
        data.put(ID.IMEI, browserContext.getRequest().getImei());
        data.put(ID.LCD_WIDTH, Integer.parseInt(browserContext.getRequest().getWidth()));
        data.put(ID.LCD_HEIGHT, Integer.parseInt(browserContext.getRequest().getHeight()));
        data.put(ID.APP_TYPE, Integer.parseInt(browserContext.getRequest().getAppType()));
        data.put(ID.TOKEN, browserContext.getResponse().getToken());
        data.put(ID.SESSION_ID, browserContext.getRequest().getSessionId());
        data.put(ID.APP_ID, Integer.parseInt(browserContext.getRequest().getAppType()));
        data.put(ID.EXT_TYPE, browserContext.getRequest().getExtendModel());
        data.put(ID.PLAT_TYPE, Integer.parseInt(browserContext.getRequest().getPlatform()));
        data.put(ID.PLAY_MODE, 1);
        data.put(ID.ABILITY, 1);
        data.put(ID.DEVICE_TYPE, Integer.parseInt(browserContext.getRequest().getDeviceType()));
        data.put(ID.CLIENT_LANGUAGE, browserContext.getRequest().getClientLanguage());
        data.put(ID.AUD_TYPE, "mp3");
        data.put(ID.AUD_SMPRATE, 46000);
        data.put(ID.AUD_CHANNEL, 1);
        data.put(ID.NETWORK_TYPE, 1);
        data.put(ID.URL_TYPE, "1");
        ch.writeAndFlush(data);
    }

    private void handleAudioData(TlbData data) {
        Object audioObj = data.get(ID.AUDIO_DATA);
        if (audioObj == null) return;
        byte[] audioBytes;
        if (audioObj instanceof byte[]) {
            audioBytes = (byte[]) audioObj;
        } else if (audioObj instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) audioObj;
            audioBytes = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), audioBytes);
        } else if (audioObj instanceof String) {
            audioBytes = ((String) audioObj).getBytes();
        } else {
            return;
        }
        // 组装帧: [0x02] + audioBytes
        byte[] frame = new byte[1 + audioBytes.length];
        frame[0] = 0x02;
        System.arraycopy(audioBytes, 0, frame, 1, audioBytes.length);
        browserContext.getSession().sendBinary(frame);
    }

    private void handleVideoData(TlbData data) {
        Object videoObj = data.get(ID.VIDEO_DATA);
        Object frameTypeObj = data.get(ID.FRAME_TYPE);
        if (videoObj == null) return;
        byte[] videoBytes;
        if (videoObj instanceof byte[]) {
            videoBytes = (byte[]) videoObj;
        } else if (videoObj instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) videoObj;
            videoBytes = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), videoBytes);
        } else if (videoObj instanceof String) {
            videoBytes = ((String) videoObj).getBytes();
        } else {
            return;
        }
        byte frameType = 0;
        if (frameTypeObj instanceof Integer) {
            frameType = ((Integer) frameTypeObj).byteValue();
        } else if (frameTypeObj instanceof String) {
            try { frameType = Byte.parseByte((String) frameTypeObj); } catch (Exception ignored) {}
        }
        // 组装帧: [0x01, frameType] + videoBytes
        byte[] frame = new byte[2 + videoBytes.length];
        frame[0] = 0x01;
        frame[1] = frameType;
        System.arraycopy(videoBytes, 0, frame, 2, videoBytes.length);
        browserContext.getSession().sendBinary(frame);
    }

    public void close() {
        if (channel != null) {
            channel.close();
        }
        group.shutdownGracefully();
    }
}
