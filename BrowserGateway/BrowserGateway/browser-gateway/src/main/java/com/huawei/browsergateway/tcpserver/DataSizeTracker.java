package com.huawei.browsergateway.tcpserver;

import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.entity.operate.Traffic;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.util.DateTimeUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据流量追踪器
 * 定期统计并上报TCP连接的数据流量信息
 */
public class DataSizeTracker {
    private static final Logger log = LogManager.getLogger(DataSizeTracker.class);
    private static final String KEY_SEPARATOR = "@";
    private static final int BATCH_SIZE = 1000;

    private final ConcurrentHashMap<String, AtomicLong> dataSizeMap = new ConcurrentHashMap<>();
    private final IRemote remote;
    private final String serviceType;
    private ScheduledExecutorService scheduler;

    @Value("${browsergw.scheduled.data-size-report-period:300000}")
    private long reportPeriodMillis;

    /**
     * 用户标识符内部类
     * 封装用户的唯一标识信息
     */
    private static class UserIdentifier {
        private String imeiAndImsi;
        private int appType;
        private String clientIP;

        /**
         * 格式化为字符串键
         */
        String format() {
            return imeiAndImsi + KEY_SEPARATOR + appType + KEY_SEPARATOR + clientIP;
        }

        /**
         * 从字符串键解析用户标识
         *
         * @param key 组合键
         * @return 解析成功返回true，失败返回false
         */
        boolean fromString(String key) {
            String[] parts = key.split(KEY_SEPARATOR);
            if (parts.length != 3) {
                log.error("Invalid user identifier key format: {}", key);
                return false;
            }
            this.imeiAndImsi = parts[0];
            this.appType = Integer.parseInt(parts[1]);
            this.clientIP = parts[2];
            return true;
        }
    }

    public DataSizeTracker(IRemote remote, String serviceType) {
        this.remote = remote;
        this.serviceType = serviceType;
    }

    /**
     * 初始化定时任务
     */
    @PostConstruct
    public void init() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(
                this::sendAllTrafficStatInfo,
                0,
                reportPeriodMillis,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 添加数据流量统计
     *
     * @param imeiAndImsi 设备标识
     * @param appType 应用类型
     * @param clientIP 客户端IP
     * @param dataSize 数据大小（字节）
     */
    public void addDataSize(String imeiAndImsi, int appType, String clientIP, int dataSize) {
        UserIdentifier userIdentifier = new UserIdentifier();
        userIdentifier.imeiAndImsi = imeiAndImsi;
        userIdentifier.appType = appType;
        userIdentifier.clientIP = clientIP;

        String key = userIdentifier.format();
        dataSizeMap.computeIfAbsent(key, k -> new AtomicLong())
                .addAndGet(dataSize);
    }

    /**
     * 发送所有流量统计信息
     * 定期执行，将累计的流量数据上报到远程服务
     */
    public void sendAllTrafficStatInfo() {
        log.info("Start sending {} traffic data", serviceType);

        List<Traffic> trafficList = collectTrafficData();

        log.info("Sending {} traffic data, collected {} records", serviceType, trafficList.size());

        if (!trafficList.isEmpty()) {
            sendTrafficDataInBatches(trafficList);
        }
    }

    /**
     * 收集流量数据
     */
    private List<Traffic> collectTrafficData() {
        List<Traffic> trafficList = new ArrayList<>();
        long endMillis = System.currentTimeMillis();
        long startMillis = endMillis - reportPeriodMillis;
        String startedAt = DateTimeUtil.millisToDate(startMillis);
        String endedAt = DateTimeUtil.millisToDate(endMillis);

        log.info("Collecting {} traffic data, map size: {}", serviceType, dataSizeMap.size());

        Iterator<Map.Entry<String, AtomicLong>> iterator = dataSizeMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, AtomicLong> entry = iterator.next();

            if (entry.getValue().get() <= 0) {
                continue;
            }

            UserIdentifier userIdentifier = new UserIdentifier();
            if (!userIdentifier.fromString(entry.getKey())) {
                continue;
            }

            Traffic traffic = new Traffic(
                    userIdentifier.imeiAndImsi,
                    userIdentifier.appType,
                    startedAt,
                    endedAt,
                    entry.getValue().get(),
                    userIdentifier.clientIP
            );
            trafficList.add(traffic);
            iterator.remove();
        }

        return trafficList;
    }

    /**
     * 分批发送流量数据
     */
    private void sendTrafficDataInBatches(List<Traffic> trafficList) {
        for (int i = 0; i < trafficList.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, trafficList.size());
            List<Traffic> batch = trafficList.subList(i, endIndex);

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("items", batch);
            String dataJson = JSONUtil.toJsonStr(dataMap);

            if (Constant.TCP_MEDIA.equals(serviceType)) {
                remote.sendTrafficMedia(dataJson);
            } else {
                remote.sendTrafficControl(dataJson);
            }
        }
    }

    /**
     * 销毁定时任务
     */
    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}
