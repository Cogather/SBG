package com.huawei.browsergateway.util.encode;

import com.huawei.browsergateway.service.TpusedMediaAccumulator;
import com.huawei.browsergateway.tcpserver.Client;
import com.huawei.browsergateway.tcpserver.DataSizeTracker;
import com.huawei.browsergateway.tcpserver.FlowRateTracker;
import com.huawei.browsergateway.websocket.media.AudioResponse;
import com.huawei.browsergateway.websocket.media.VideoResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.ByteOrder;

import static com.huawei.browsergateway.tcpserver.Client.VAL_APP_TYPE;
import static com.huawei.browsergateway.tcpserver.Client.VAL_SESSION_ID;

/** TLV 协议编码器，将对象或字节数组写入 {@link ByteBuf} */
public class TlvEncoder extends MessageToByteEncoder<Object> {

    private final DataSizeTracker dataSizeTracker;
    private final FlowRateTracker flowRateTracker;
    private final String serviceType;
    private final TpusedMediaAccumulator tpusedAccumulator;

    public TlvEncoder(DataSizeTracker dataSizeTracker, FlowRateTracker flowRateTracker, String serviceType) {
        this(dataSizeTracker, flowRateTracker, serviceType, null);
    }

    public TlvEncoder(DataSizeTracker dataSizeTracker, FlowRateTracker flowRateTracker, String serviceType,
                      TpusedMediaAccumulator tpusedAccumulator) {
        this.dataSizeTracker = dataSizeTracker;
        this.flowRateTracker = flowRateTracker;
        this.serviceType = serviceType;
        this.tpusedAccumulator = tpusedAccumulator;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        Client cli = Client.fromCtx(ctx);
        String sessionId = cli.getStr(VAL_SESSION_ID);
        int appType = cli.getInt(VAL_APP_TYPE);
        String clientIp = cli.getClientIpAddress();

        addTpusedPayloadIfApplicable(msg);

        byte[] bytes;
        if (msg instanceof byte[]) {
            bytes = (byte[]) msg;
        } else {
            bytes = TlvCodec.marshal(msg).marshal(ByteOrder.BIG_ENDIAN);
        }

        dataSizeTracker.addDataSize(sessionId, appType, clientIp, bytes.length);
        flowRateTracker.add(sessionId, serviceType, bytes.length);
        out.writeBytes(bytes);
    }

    private void addTpusedPayloadIfApplicable(Object msg) {
        if (tpusedAccumulator == null) {
            return;
        }
        if (msg instanceof VideoResponse) {
            byte[] vd = ((VideoResponse) msg).getVideoData();
            tpusedAccumulator.addPayloadBytes(vd == null ? 0 : vd.length);
        } else if (msg instanceof AudioResponse) {
            byte[] ad = ((AudioResponse) msg).getAudioData();
            tpusedAccumulator.addPayloadBytes(ad == null ? 0 : ad.length);
        }
    }
}
