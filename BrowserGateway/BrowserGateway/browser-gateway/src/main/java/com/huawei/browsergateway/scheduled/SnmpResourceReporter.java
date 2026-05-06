package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.adapter.ResourceMonitorAdapter;
import com.huawei.browsergateway.adapter.dto.MeasureItem;
import com.huawei.browsergateway.adapter.dto.SnmpPerfRequest;
import com.huawei.browsergateway.adapter.http.SnmpHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 资源负载定时上报任务，周期性采集 CPU/内存使用率并通过 SNMP 上报至 SFMU
 */
@Component
public class SnmpResourceReporter {

    private static final Logger log = LogManager.getLogger(SnmpResourceReporter.class);

    private static final String CPU_MEASURE_UNIT = "320301";
    private static final String MEMORY_MEASURE_UNIT = "320302";
    private static final String MEASURE_ENTITY = "1";
    private static final String OPT_TYPE = "set";

    @Autowired
    private ResourceMonitorAdapter resourceMonitorAdapter;

    @Autowired
    private SnmpHttpClient snmpHttpClient;

    @Value("${browsergw.snmp.resource.report-period:300000}")
    private long period;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::reportResourceLoad, 0, period, TimeUnit.MILLISECONDS);
        log.info("SNMP resource reporter initialized, period: {}ms", period);
    }

    public void reportResourceLoad() {
        try {
            float cpuUsage = resourceMonitorAdapter.getCpuUsage();
            float memoryUsage = resourceMonitorAdapter.getMemoryUsage();

            String localIP = snmpHttpClient.getLocalIP();
            String objectName = "bgw-" + localIP;

            List<MeasureItem> measureList = new ArrayList<>();
            measureList.add(new MeasureItem(CPU_MEASURE_UNIT, MEASURE_ENTITY, objectName, cpuUsage, OPT_TYPE));
            measureList.add(new MeasureItem(MEMORY_MEASURE_UNIT, MEASURE_ENTITY, objectName, memoryUsage, OPT_TYPE));

            SnmpPerfRequest request = new SnmpPerfRequest();
            request.setMeasureList(measureList);

            snmpHttpClient.sendPerf(request);

            log.info("Resource load reported: cpu={}%, memory={}%", cpuUsage, memoryUsage);
        } catch (Exception e) {
            log.error("Report resource load error", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
            log.info("SNMP resource reporter destroyed.");
        }
    }
}