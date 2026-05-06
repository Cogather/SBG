package com.huawei.browsergateway.adapter.http;

import com.huawei.browsergateway.adapter.dto.SnmpPerfRequest;
import com.huawei.browsergateway.config.SnmpConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.util.UUID;

/**
 * SNMP HTTP客户端
 * 用于发送话统数据到SFMU SNMP模块
 */
@Component
public class SnmpHttpClient {

    private static final Logger log = LogManager.getLogger(SnmpHttpClient.class);

    private static final String PERF_PATH = "/v1/app/perf";

    @Autowired
    private SnmpConfig snmpConfig;

    private final RestTemplate restTemplate;

    public SnmpHttpClient() {
        this.restTemplate = new RestTemplate();
    }

    public void sendPerf(SnmpPerfRequest request) {
        String[] serverIPs = snmpConfig.getServerIPs();

        for (String serverIP : serverIPs) {
            if (serverIP == null || serverIP.isEmpty()) {
                continue;
            }

            String url = String.format("http://%s:%d%s", serverIP, snmpConfig.getServerPort(), PERF_PATH);

            for (int retry = 0; retry < snmpConfig.getRetryTimes(); retry++) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("transaction-id", UUID.randomUUID().toString());
                    headers.set("kpi-type", "VM");
                    headers.set("peer-ip", getLocalIP());
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    HttpEntity<SnmpPerfRequest> entity = new HttpEntity<>(request, headers);
                    ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("SNMP perf sent successfully to {}", url);
                        return;
                    }
                } catch (Exception e) {
                    log.warn("SNMP perf failed to {}, retry {}: {}", url, retry + 1, e.getMessage());
                    try {
                        Thread.sleep(snmpConfig.getRetryInterval());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        log.error("All SNMP servers failed for perf request");
    }

    public String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("Failed to get local IP", e);
            return "unknown";
        }
    }
}