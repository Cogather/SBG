package com.huawei.mobile;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.huawei.mobile.common.ID;
import com.huawei.mobile.common.Type;
import com.huawei.mobile.dto.CallbackMessage;
import com.huawei.mobile.encode.TlbData;
import com.huawei.mobile.encode.TlvData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ControlChannelHandler extends ChannelInboundHandlerAdapter {
    private static final Log log = LogFactory.get();

    private volatile CountDownLatch latch;
    private volatile int ackType;
    private final BrowserContext demo;

    public ControlChannelHandler(BrowserContext demo) {
        this.demo = demo;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
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
                if (ackType != Type.HEARTBEATS) {
                    if (this.ackType == ackType) {
                        latch.countDown();
                    }
                }
                break;
            case Type.RETURN_MEDIA:
                ByteBuf byteBuf = data.get(ID.TCP_ADDR);
                String addr = byteBuf.toString(StandardCharsets.UTF_8);
                demo.callbackMediaAddr(addr);
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
                break;
            case Type.RETURN_CONTROL:
                int elm = data.get(ID.CTRL_RSP_ELM).readInt();
                int info = data.get(ID.CTRL_RSP_INFO).readInt();
                String content = data.get(ID.CONTENT).toString(StandardCharsets.UTF_8);
//                int wt = data.get(ID.WRITE_TYPE).readInt();
                CallbackMessage cm = new CallbackMessage();
                cm.setType("callback");
                cm.setElm(elm);
                cm.setInfo(info);
                cm.setContent(content);
//                cm.setWt(wt);
                log.info("receive return control message: {}", cm);
                demo.callbackMessage(cm);
                break;
            default:
                break;
        }

        data.releaseAll();
    }

    public void send(Channel channel, TlvData tlvData, int type) throws InterruptedException {
        log.info("send control message type:{}", type);
        this.latch = new CountDownLatch(1);
        this.ackType = type;

        channel.writeAndFlush(tlvData);
        this.latch.await();
        log.info("success to wait ack for type:{}", type);
    }
}