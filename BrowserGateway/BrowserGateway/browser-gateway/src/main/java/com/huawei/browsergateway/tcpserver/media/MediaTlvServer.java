package com.huawei.browsergateway.tcpserver.media;

import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.service.IRemote;
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

@Component
public class MediaTlvServer extends AbstractTcpServer {
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

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected Integer getPort() {
        return config.getTcp().getMediaPort();
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
        return new TlvEncoder(mediaDataSizeTracker, flowRateTracker, Constant.MEDIA_SERVICE_TYPE);
    }

    @Override
    protected ChannelHandler getDecoder() {
        return new TlvDecoder(Constant.TCP_DECODER_MAX_SIZE, flowRateTracker, Constant.MEDIA_SERVICE_TYPE);
    }

    @PostConstruct
    public void startServer() {
        if (!config.getTcp().isEnableHttp()) {
            log.info("get env enableHttp if false, not start tlv");
            return;
        }
        log.info("get env enableHttp if true, start tlv server");
        start(false);
    }

    @PreDestroy
    public void stopServer() {
        stop();
    }
}