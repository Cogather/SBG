package com.huawei.browsergateway.adapter.dto;

import lombok.Data;

import java.util.List;

/**
 * SNMP话统上报请求
 * 用于上报话统数据到SFMU SNMP模块
 */
@Data
public class SnmpPerfRequest {
    
    private List<MeasureItem> measureList;
}