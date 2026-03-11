package com.huawei.browsergateway.adapter;

import com.huawei.browsergateway.adapter.dto.AlarmRequest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AlarmAdapter 测试
 * 测试告警适配器的核心功能
 */
class AlarmAdapterTest {

    @Test
    void testAlarmType枚举值() {
        // Then
        assertNotNull(AlarmAdapter.AlarmType.GENERATE, "GENERATE类型应存在");
        assertNotNull(AlarmAdapter.AlarmType.CLEAR, "CLEAR类型应存在");
        assertEquals(2, AlarmAdapter.AlarmType.values().length, "应有2种告警类型");
    }

    @Test
    void testAlarmRequest创建() {
        // Given
        AlarmRequest request = new AlarmRequest();
        request.setAlarmId("ALARM_001");
        request.setType(AlarmAdapter.AlarmType.GENERATE);

        Map<String, String> params = new HashMap<>();
        params.put("resource", "CPU");
        request.setParameters(params);

        // Then
        assertEquals("ALARM_001", request.getAlarmId(), "告警ID应匹配");
        assertEquals(AlarmAdapter.AlarmType.GENERATE, request.getType(), "告警类型应匹配");
        assertNotNull(request.getParameters(), "参数不应为null");
    }

    @Test
    void testAlarmRequest批量创建() {
        // Given
        AlarmRequest request1 = new AlarmRequest();
        request1.setAlarmId("ALARM_001");

        AlarmRequest request2 = new AlarmRequest();
        request2.setAlarmId("ALARM_002");

        List<AlarmRequest> requests = Arrays.asList(request1, request2);

        // Then
        assertEquals(2, requests.size(), "应有2个告警请求");
        assertNotNull(requests.get(0).getAlarmId(), "第一个告警ID不应为null");
        assertNotNull(requests.get(1).getAlarmId(), "第二个告警ID不应为null");
    }
}
