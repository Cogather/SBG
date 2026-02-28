package com.huawei.browsergateway.tcpserver.control;

import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.service.IChromeSet;
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
public class ControlTlvServer extends AbstractTcpServer {
    private static final Logger log = LogManager.getLogger(ControlTlvServer.class);

    @Autowired
    private Config config;
    @Autowired
    private IRemote remote;
    @Autowired
    private IChromeSet chromeSet;
    @Autowired
    private ControlClientSet cs;
    @Autowired
    private FlowRateTracker flowRateTracker;

    @Autowired
    @Qualifier("controlDataSizeTracker")
    private DataSizeTracker controlDataSizeTracker;

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected Integer getPort() {
        return config.getTcp().getControlPort();
    }

    @Override
    protected String getAddress() {
        return config.getTcp().getAddress();
    }

    @Override
    protected ChannelHandler getHandler() {
        return new ControlTcpServerHandler(remote, cs, chromeSet, flowRateTracker);
    }

    @Override
    protected ChannelHandler getEncoder() {
        return new TlvEncoder(controlDataSizeTracker, flowRateTracker, Constant.CONTROL_SERVICE_TYPE);
    }

    @Override
    protected ChannelHandler getDecoder() {
        return new TlvDecoder(Constant.TCP_DECODER_MAX_SIZE, flowRateTracker, Constant.CONTROL_SERVICE_TYPE);
    }

    @PostConstruct
    public void startServer() {
        if (!config.getTcp().isEnableHttp()) {
            log.info("get env enableHttp if false, not start Tlv");
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
