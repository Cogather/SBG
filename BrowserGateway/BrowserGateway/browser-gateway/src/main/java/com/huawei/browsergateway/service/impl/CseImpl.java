package com.huawei.browsergateway.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import org.apache.servicecomb.registry.api.registry.MicroserviceInstance;
import org.apache.servicecomb.registry.api.registry.MicroserviceInstanceStatus;
import org.apache.servicecomb.serviceregistry.RegistryUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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


    @Override
    public String getReportEndpoint() {
        List<MicroserviceInstance> instances = RegistryUtils.findServiceInstance("0", "gids", "0+");
        if (CollectionUtil.isEmpty(instances)) {
            return "";
        }
        HashSet<String> endpoints = new HashSet<>();
        for (MicroserviceInstance instance : instances) {
            if (instance.getStatus() != MicroserviceInstanceStatus.UP) {
                continue;
            }
            List<String> instanceEndpoints = instance.getEndpoints();
            if (CollectionUtil.isEmpty(instanceEndpoints)) {
                continue;
            }
            for (String endpoint : instanceEndpoints) {
                String ipPort = extractIPPort(endpoint);
                if (ipPort == null) {
                    continue;
                }
                endpoints.add(ipPort);
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
