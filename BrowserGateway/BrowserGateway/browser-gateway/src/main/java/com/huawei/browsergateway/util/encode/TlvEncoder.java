package com.huawei.browsergateway.util.encode;

import com.huawei.browsergateway.tcpserver.Client;
import com.huawei.browsergateway.tcpserver.DataSizeTracker;
import com.huawei.browsergateway.tcpserver.FlowRateTracker;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.ByteOrder;

import static com.huawei.browsergateway.tcpserver.Client.VAL_APP_TYPE;
import static com.huawei.browsergateway.tcpserver.Client.VAL_SESSION_ID;

public class TlvEncoder extends MessageToByteEncoder<Object> {

    private final DataSizeTracker dataSizeTracker;
    private final FlowRateTracker flowRateTracker;
    private final String serviceType;
    public TlvEncoder(DataSizeTracker dataSizeTracker, FlowRateTracker flowRateTracker, String serviceType) {
        this.dataSizeTracker = dataSizeTracker;
        this.flowRateTracker = flowRateTracker;
        this.serviceType = serviceType;
    }


    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        Client cli = Client.fromCtx(ctx);
        String sessionID = cli.getStr(VAL_SESSION_ID);
        int appType = cli.getInt(VAL_APP_TYPE);
        String clientIP = cli.getClientIpAddress();

        if (msg instanceof byte[]) {
            this.dataSizeTracker.addDataSize(sessionID, appType, clientIP, ((byte[]) msg).length);
            this.flowRateTracker.add(sessionID, this.serviceType, ((byte[]) msg).length);
            out.writeBytes((byte[]) msg);
        } else {
            Tlv tlv = TlvCodec.marshal(msg);
            byte[] marshal = tlv.marshal(ByteOrder.BIG_ENDIAN);
            this.dataSizeTracker.addDataSize(sessionID, appType, clientIP, marshal.length);
            this.flowRateTracker.add(sessionID, this.serviceType, marshal.length);
            out.writeBytes(marshal);
        }
    }
}