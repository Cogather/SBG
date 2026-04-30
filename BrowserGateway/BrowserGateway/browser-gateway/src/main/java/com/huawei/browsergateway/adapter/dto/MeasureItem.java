package com.huawei.browsergateway.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SNMP话统上报请求项
 * 用于上报单个话统数据项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeasureItem {
    
    private String measureUnit;
    private String measureEntity;
    private String objectName;
    private Object optValue;
    private String optType;
}