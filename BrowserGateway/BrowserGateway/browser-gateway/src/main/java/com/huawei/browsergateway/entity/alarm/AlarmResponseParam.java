package com.huawei.browsergateway.entity.alarm;

import lombok.Data;

import java.util.List;

/**
 * 告警响应参数
 */
@Data
public class AlarmResponseParam {
    /** 返回描述 */
    private String retdesc;

    /** 告警数据列表 */
    private List<DataParam> data;

    /** 数据总数 */
    private int totalNum;
}
