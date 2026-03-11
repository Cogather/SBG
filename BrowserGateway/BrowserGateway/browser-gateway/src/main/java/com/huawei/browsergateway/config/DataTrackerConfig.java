package com.huawei.browsergateway.config;

import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.tcpserver.DataSizeTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据追踪配置类
 * 负责配置数据流量追踪相关的Bean，包括媒体和控制通道的数据流量追踪器
 */
@Configuration
public class DataTrackerConfig {
    /**
     * 创建媒体数据流量追踪器Bean
     * <p>
     * 用于追踪和统计TCP媒体通道的数据流量，并定期上报统计数据
     *
     * @return 媒体数据流量追踪器实例
     */
    @Bean(name = "mediaDataSizeTracker")
    public DataSizeTracker  mediaDataSizeTracker(IRemote remote) {
        return new DataSizeTracker(remote, Constant.TCP_MEDIA);
    }

    /**
     * 创建控制数据流量追踪器Bean
     * <p>
     * 用于追踪和统计TCP控制通道的数据流量，并定期上报统计数据
     *
     * @return 控制数据流量追踪器实例
     */
    @Bean(name = "controlDataSizeTracker")
    public DataSizeTracker  controlDataSizeTracker(IRemote remote) {
        return new DataSizeTracker(remote, Constant.TCP_CONTROL);
    }
}
