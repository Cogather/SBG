package com.huawei.browsergateway.config;

import com.huawei.browsergateway.common.Constants;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.service.impl.DataSizeTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 数据追踪配置类
 * 对应存量代码中的DataTrackerConfig类
 */
@Configuration
public class DataTrackerConfig {

    @Autowired(required = false)
    private IRemote remote;

    @Bean
    public DataSizeTracker mediaDataSizeTracker() {
        return new DataSizeTracker(remote, Constants.TCP_MEDIA);
    }

    @Bean
    public DataSizeTracker controlDataSizeTracker() {
        return new DataSizeTracker(remote, Constants.TCP_CONTROL);
    }
}
