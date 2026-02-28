package com.huawei.browsergateway.tcpserver;

import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.common.Constant;
import com.huawei.browsergateway.entity.operate.Traffic;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.util.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DataSizeTracker {
    private static final Logger log = LoggerFactory.getLogger(DataSizeTracker.class);
    private final ConcurrentHashMap<String, AtomicLong> dataSizeMap = new ConcurrentHashMap();
    private static final String splitKey = "@";
    private static final int batchSize = 1000;

    private static class UserIdentifier {
        public String imeiAndImsi;
        public int appType;
        public String clientIP;

        public String format() {
            return this.imeiAndImsi + splitKey + this.appType + splitKey + this.clientIP;
        }

        public Boolean fromString(String key) {
            String[] parts = key.split(splitKey);
            if (parts.length != 3) {
                log.error("Invalid combined key format: {}", key);
                return false;
            }
            this.imeiAndImsi = parts[0];
            this.appType = Integer.parseInt(parts[1]);
            this.clientIP = parts[2];
            return true;
        }
    }

    @Value("${browsergw.scheduled.data-size-report-period:300000}")
    private long period;
    private final IRemote remote;
    private final String type;
    private ScheduledExecutorService scheduler;

    public DataSizeTracker(IRemote remote, String type) {
        this.remote = remote;
        this.type = type;
    }

    @PostConstruct
    public void init() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(this::SendAllTrafficStatInfo, 0, period, TimeUnit.MILLISECONDS);
    }

    public void addDataSize(String imeiAndImsi, int appType, String clientIP, int dataSize) {
        UserIdentifier userIdentifier = new UserIdentifier();
        userIdentifier.imeiAndImsi = imeiAndImsi;
        userIdentifier.appType = appType;
        userIdentifier.clientIP = clientIP;
        this.dataSizeMap.computeIfAbsent(userIdentifier.format(), (key) -> new AtomicLong()).addAndGet(dataSize);
    }

    public void SendAllTrafficStatInfo() {
        log.info("send {} Traffic Data Begin ", this.type);
        List<Traffic> trafficList = new ArrayList<>();
        long endMillis = System.currentTimeMillis();
        long startMillis = endMillis - period;
        String startedAt = DateTimeUtil.millisToDate(startMillis);
        String endedAt = DateTimeUtil.millisToDate(endMillis);

        log.info("send {} Traffic Data, map size is={}", this.type, dataSizeMap.size());
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
            Traffic traffic = new Traffic(userIdentifier.imeiAndImsi, String.valueOf(userIdentifier.appType), startedAt, endedAt,
                    entry.getValue().get(), userIdentifier.clientIP);
            trafficList.add(traffic);
            iterator.remove();
        }

        log.info("send {} Traffic Data, list size is={}", this.type, trafficList.size());
        if (!trafficList.isEmpty()) {
            // 分批发送数据
            for (int i = 0; i < trafficList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, trafficList.size());
                List<Traffic> batch = trafficList.subList(i, end);
                Map<String, Object> dataMap = new HashMap<>();
                dataMap.put("items", batch);
                String dataJson = JSONUtil.toJsonStr(dataMap);
                if (Constant.TCP_MEDIA.equals(type)) {
                    remote.sendTrafficMedia(dataJson);
                } else {
                    remote.sendTrafficControl(dataJson);
                }
            }
        }
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}