package com.huawei.browsergateway.entity.alarm;

import lombok.Data;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 告警响应参数
 */
@Data
public class AlarmResponseParam {

    private String retdesc;

    private List<DataParam> data;

    private int totalNum;

}