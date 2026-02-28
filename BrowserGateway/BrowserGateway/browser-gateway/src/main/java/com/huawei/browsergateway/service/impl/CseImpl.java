package com.huawei.browsergateway.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.huawei.browsergateway.adapter.dto.ServiceInstance;
import com.huawei.browsergateway.adapter.interfaces.ServiceManagementAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

@Service
public class CseImpl implements com.huawei.browsergateway.service.ICse {
    private static final Logger log = LogManager.getLogger(CseImpl.class);
    private static final Random random = new Random();
    
    @Autowired
    private ServiceManagementAdapter serviceManagementAdapter;

    public String getReportEndpoint() {
        if (serviceManagementAdapter == null) {
            log.warn("ServiceManagementAdapter is not available");
            return "";
        }
        
        List<ServiceInstance> instances = serviceManagementAdapter.findServiceInstances("0", "gids", "0+");
        if (CollectionUtil.isEmpty(instances)) {
            return "";
        }
        HashSet<String> endpoints = new HashSet<>();
        for (ServiceInstance instance : instances) {
            if (instance.getStatus() != ServiceInstance.InstanceStatus.UP) {
                continue;
            }
            List<String> instanceEndpoints = instance.getEndpoints();
            if (instanceEndpoints != null) {
                for (String endpoint : instanceEndpoints) {
                    String ipPort = extractIPPort(endpoint);
                    if (ipPort == null) {
                        continue;
                    }
                    endpoints.add(ipPort);
                }
            }
        }
        if (endpoints.isEmpty()) {
            return "";
        }
        Object[] array = endpoints.toArray();
        return (String) array[random.nextInt(array.length)];
    }

    private String extractIPPort(String endpoint) {
        try {
            URI uri = new URI(endpoint);
            return uri.getHost() + ":" + uri.getPort();

        } catch (URISyntaxException e) {
            log.error("failed to parse endpoint {}, err: {}", endpoint, e.getMessage(), e);
            return null;
        }
    }
}
