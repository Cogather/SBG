package com.huawei.browsergateway.service;

import com.huawei.browsergateway.adapter.dto.MeasureItem;
import com.huawei.browsergateway.adapter.dto.SnmpPerfRequest;
import com.huawei.browsergateway.adapter.http.SnmpHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;

/**
 * SNMP初始化服务
 * 在启动时发送测试话统数据，验证SNMP连通性
 */
@Component
public class SnmpInitService {

    private static final Logger log = LogManager.getLogger(SnmpInitService.class);

    @Autowired
    private SnmpHttpClient snmpHttpClient;

    @PostConstruct
    public void init() {
        log.info("Initializing SNMP client...");

        try {
            sendTestPerf();
            log.info("BGW SNMP connectivity test successful");
        } catch (Exception e) {
            log.error("BGW SNMP connectivity test failed: {}", e.getMessage());
        }
    }

    private void sendTestPerf() {
        SnmpPerfRequest testRequest = new SnmpPerfRequest();
        testRequest.setMeasureList(Arrays.asList(
            new MeasureItem("TEST", "1", "bgw-test", 0, "set")
        ));

        snmpHttpClient.sendPerf(testRequest);
    }
}