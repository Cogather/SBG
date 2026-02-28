package com.huawei.browsergateway.entity.alarm;

import lombok.Data;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
public class AlarmResponseParam {

    private String retdesc;

    private List<DataParam> data;

    private int totalNum;

}