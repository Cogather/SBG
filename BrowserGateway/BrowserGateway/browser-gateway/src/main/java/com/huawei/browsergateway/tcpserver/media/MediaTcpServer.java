package com.huawei.browsergateway.tcpserver.media;

import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.service.TpusedMediaAccumulator;
import com.huawei.browsergateway.tcpserver.AbstractTcpServer;
import com.huawei.browsergateway.tcpserver.DataSizeTracker;
import com.huawei.browsergateway.tcpserver.FlowRateTracker;
import com.huawei.browsergateway.util.encode.TlvDecoder;
import com.huawei.browsergateway.util.encode.TlvEncoder;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 媒体流TCP服务器（TLS加密）
 * 处理客户端的媒体数据传输
 */
@Component
public class MediaTcpServer extends AbstractTcpServer {
    private static final Logger log = LogManager.getLogger(MediaTcpServer.class);

    @Autowired
    private Config config;

    @Autowired
    private MediaClientSet mediaClientSet;

    @Autowired
    private IRemote remote;

    @Autowired
    private FlowRateTracker flowRateTracker;

    @Autowired
    @Qualifier("mediaDataSizeTracker")
    private DataSizeTracker mediaDataSizeTracker;

    @Autowired
    private TpusedMediaAccumulator tpusedMediaAccumulator;

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected Integer getPort() {
        return config.getTcp().getMediaTlsPort();
    }

    @Override
    protected String getAddress() {
        return config.getTcp().getAddress();
    }

    @Override
    protected ChannelHandler getHandler() {
        return new MediaTcpServerHandle(mediaClientSet, remote, flowRateTracker);
    }

    @Override
    protected ChannelHandler getEncoder() {
        return new TlvEncoder(mediaDataSizeTracker, flowRateTracker, Constant.MEDIA_SERVICE_TYPE, tpusedMediaAccumulator);
    }

    @Override
    protected ChannelHandler getDecoder() {
        return new TlvDecoder(Constant.TCP_DECODER_MAX_SIZE, flowRateTracker, Constant.MEDIA_SERVICE_TYPE);
    }

    /**
     * 启动媒体流TLS服务器
     */
    @PostConstruct
    public void startServer() {
        start(true);
    }

    /**
     * 停止媒体流服务器
     */
    @PreDestroy
    public void stopServer() {
        stop();
    }
}
