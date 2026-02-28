package com.huawei.browsergateway.service.healthCheck;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;

public class NetWorkInterfaceCheck implements ICheckStrategy {
    private static final Logger log = LogManager.getLogger(NetWorkInterfaceCheck.class);

    // todo：待增加trunck和fabric平面
    private final List<String> interfaceList = Arrays.asList("bond-base", "bond-external");

    @Override
    public HealthCheckResult check() {
        boolean isHealthy = true;
        StringBuilder errMsg = new StringBuilder();

        for (String name : interfaceList) {
            try {
                NetworkInterface ni = NetworkInterface.getByName(name);
                if (ni == null) {
                    isHealthy = false;
                    errMsg.append(String.format("interface %s not found;", name));
                } else if (!ni.isUp()) {
                    isHealthy = false;
                    errMsg.append(String.format("interface %s is down;", name));
                }
            } catch (SocketException e) {
                log.error("get interface error", e);
            }
        }
        if (!isHealthy) {
            log.warn("[NetWorkInterfaceCheck] check result: {}", errMsg);
        }
        HealthCheckResult result = new HealthCheckResult();
        result.setHealthy(isHealthy);
        result.setErrorMsg(errMsg.toString());
        result.setCheckItem("NetWorkInterface");
        return result;
    }
}