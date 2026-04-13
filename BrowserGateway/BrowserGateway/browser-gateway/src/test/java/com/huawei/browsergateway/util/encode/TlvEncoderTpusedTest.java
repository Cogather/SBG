package com.huawei.browsergateway.util.encode;

import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.service.TpusedMediaAccumulator;
import com.huawei.browsergateway.tcpserver.Client;
import com.huawei.browsergateway.tcpserver.DataSizeTracker;
import com.huawei.browsergateway.tcpserver.FlowRateTracker;
import com.huawei.browsergateway.websocket.media.AudioResponse;
import com.huawei.browsergateway.websocket.media.VideoResponse;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.huawei.browsergateway.tcpserver.Client.VAL_APP_TYPE;
import static com.huawei.browsergateway.tcpserver.Client.VAL_SESSION_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TlvEncoderTpusedTest {

    @Mock
    private DataSizeTracker dataSizeTracker;

    @Mock
    private FlowRateTracker flowRateTracker;

    private TpusedMediaAccumulator tpusedMediaAccumulator;
    private TlvEncoder tlvEncoder;

    @BeforeEach
    void setUp() {
        tpusedMediaAccumulator = new TpusedMediaAccumulator();
        tlvEncoder = new TlvEncoder(dataSizeTracker, flowRateTracker, Constant.MEDIA_SERVICE_TYPE, tpusedMediaAccumulator);
    }

    @Test
    void videoAndAudioPayloads_accumulateTpused_onlyPayloadLengths() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(tlvEncoder);
        channel.pipeline().fireChannelRegistered();
        channel.pipeline().fireChannelActive();

        var encCtx = channel.pipeline().context(tlvEncoder);
        assertNotNull(encCtx);
        Client cli = Client.fromCtx(encCtx);
        cli.set(VAL_SESSION_ID, "sid-1");
        cli.set(VAL_APP_TYPE, 7);

        byte[] videoPayload = new byte[120];
        byte[] audioPayload = new byte[30];
        channel.writeOutbound(new VideoResponse(1, videoPayload, "user", 0));
        channel.writeOutbound(new AudioResponse(2, audioPayload, "user"));

        assertEquals(150, tpusedMediaAccumulator.getCurrentBytes());
        verify(dataSizeTracker, atLeastOnce()).addDataSize(eq("sid-1"), eq(7), isNull(), anyInt());
        verify(flowRateTracker, atLeastOnce()).add(eq("sid-1"), eq(Constant.MEDIA_SERVICE_TYPE), anyInt());
    }

    @Test
    void controlEncoder_nullAccumulator_doesNotTouchBeanInstance() throws Exception {
        TlvEncoder controlEnc = new TlvEncoder(dataSizeTracker, flowRateTracker, Constant.CONTROL_SERVICE_TYPE, null);
        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(controlEnc);
        channel.pipeline().fireChannelRegistered();
        channel.pipeline().fireChannelActive();
        var encCtx = channel.pipeline().context(controlEnc);
        Client cli = Client.fromCtx(encCtx);
        cli.set(VAL_SESSION_ID, "s");
        cli.set(VAL_APP_TYPE, 1);
        channel.writeOutbound(new VideoResponse(1, new byte[50], "u", 0));
        assertEquals(0, tpusedMediaAccumulator.getCurrentBytes());
    }
}
