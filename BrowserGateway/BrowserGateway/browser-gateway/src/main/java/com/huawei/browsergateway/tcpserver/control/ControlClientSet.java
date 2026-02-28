package com.huawei.browsergateway.tcpserver.control;

import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.tcpserver.Client;
import com.huawei.browsergateway.tcpserver.ClientSet;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 控制流客户端管理
 * 管理所有TCP控制流连接
 */
@Component
public class ControlClientSet extends ClientSet {
    @Autowired
    private IRemote remote;
    @Override
    public void set(String key, Client cli) {
        Client oldCli = super.map.get(key);
        if (oldCli != null) {
            oldCli.set(Client.HAS_BEEN_FALLBACK, "true");
            remote.fallback(key);
            super.del(oldCli);
        }
        map.put(key, cli);
    }
}