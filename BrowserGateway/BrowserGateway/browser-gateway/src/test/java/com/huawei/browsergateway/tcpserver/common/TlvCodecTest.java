package com.huawei.browsergateway.tcpserver.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteOrder;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TlvCodec单元测试
 * 基于决策表（DT）方法设计测试用例
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TlvCodec决策表测试")
class TlvCodecTest {

    private static final String TEST_IMEI = "123456789012345";
    private static final String TEST_IMSI = "123456789012345";

    @BeforeEach
    void setUp() {
        // 重置格式为默认值
        TlvCodec.setFormat(TlvCodec.TlvFormat.JSON);
    }

    /**
     * 决策表：TLV编组测试
     * 
     * 条件：
     * C1: Message是否为null
     * C2: Message类型是否有效
     * C3: Message字段是否完整
     * 
     * 动作：
     * A1: 是否抛出异常
     * A2: TlvMessage是否创建成功
     * A3: TlvMessage类型是否正确
     */
    @ParameterizedTest(name = "TLV编组决策表 - 规则{0}")
    @MethodSource("marshalDecisionTable")
    @DisplayName("TLV编组决策表测试")
    void testMarshalDecisionTable(
            String ruleId,
            Message message,
            boolean shouldThrowException,
            boolean shouldCreateTlvMessage,
            Short expectedType) {
        
        if (shouldThrowException) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                TlvCodec.marshal(message);
            });
            assertTrue(exception.getMessage().contains("消息对象不能为null"));
        } else {
            TlvMessage tlvMessage = TlvCodec.marshal(message);
            
            if (shouldCreateTlvMessage) {
                assertNotNull(tlvMessage);
                assertEquals(expectedType, tlvMessage.getType());
                assertNotNull(tlvMessage.getValue());
                assertTrue(tlvMessage.getValue().length > 0);
            }
        }
    }

    /**
     * 决策表数据源：TLV编组
     */
    static Stream<Arguments> marshalDecisionTable() {
        return Stream.of(
            // R1: Message为null -> 抛出异常
            Arguments.of("R1", null, true, false, null),
            
            // R2: Message类型为LOGIN，字段完整 -> 成功编组
            Arguments.of("R2", createMessage(MessageType.LOGIN, TEST_IMEI, TEST_IMSI), 
                    false, true, MessageType.LOGIN),
            
            // R3: Message类型为HEARTBEATS，字段完整 -> 成功编组
            Arguments.of("R3", createMessage(MessageType.HEARTBEATS, TEST_IMEI, TEST_IMSI), 
                    false, true, MessageType.HEARTBEATS),
            
            // R4: Message类型为LOGOUT，字段完整 -> 成功编组
            Arguments.of("R4", createMessage(MessageType.LOGOUT, TEST_IMEI, TEST_IMSI), 
                    false, true, MessageType.LOGOUT),
            
            // R5: Message字段为空 -> 成功编组（空值）
            Arguments.of("R5", createEmptyMessage(MessageType.LOGIN), 
                    false, true, MessageType.LOGIN)
        );
    }

    /**
     * 决策表：TLV解码测试
     * 
     * 条件：
     * C1: TlvMessage是否为null
     * C2: TlvMessage类型是否有效
     * C3: TlvMessage值是否为空
     * 
     * 动作：
     * A1: 是否抛出异常
     * A2: Message是否创建成功
     * A3: Message类型是否正确
     */
    @ParameterizedTest(name = "TLV解码决策表 - 规则{0}")
    @MethodSource("unmarshalDecisionTable")
    @DisplayName("TLV解码决策表测试")
    void testUnmarshalDecisionTable(
            String ruleId,
            TlvMessage tlvMessage,
            boolean shouldThrowException,
            boolean shouldCreateMessage,
            Short expectedType) {
        
        if (shouldThrowException) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                TlvCodec.unmarshal(tlvMessage);
            });
            assertTrue(exception.getMessage().contains("TLV消息对象不能为null"));
        } else {
            Message message = TlvCodec.unmarshal(tlvMessage);
            
            if (shouldCreateMessage) {
                assertNotNull(message);
                assertEquals(expectedType, message.getType());
            }
        }
    }

    /**
     * 决策表数据源：TLV解码
     */
    static Stream<Arguments> unmarshalDecisionTable() {
        return Stream.of(
            // R1: TlvMessage为null -> 抛出异常
            Arguments.of("R1", null, true, false, null),
            
            // R2: TlvMessage类型为LOGIN，值有效 -> 成功解码
            Arguments.of("R2", createTlvMessage(MessageType.LOGIN, TEST_IMEI, TEST_IMSI), 
                    false, true, MessageType.LOGIN),
            
            // R3: TlvMessage类型为HEARTBEATS，值有效 -> 成功解码
            Arguments.of("R3", createTlvMessage(MessageType.HEARTBEATS, TEST_IMEI, TEST_IMSI), 
                    false, true, MessageType.HEARTBEATS),
            
            // R4: TlvMessage值为空 -> 成功解码（空字段）
            Arguments.of("R4", createEmptyTlvMessage(MessageType.LOGIN), 
                    false, true, MessageType.LOGIN)
        );
    }

    /**
     * 决策表：编解码往返测试
     * 
     * 条件：
     * C1: 原始Message类型
     * C2: 原始Message字段
     * 
     * 动作：
     * A1: 编解码后Message是否与原始一致
     */
    @ParameterizedTest(name = "编解码往返决策表 - 规则{0}")
    @MethodSource("roundTripDecisionTable")
    @DisplayName("编解码往返决策表测试")
    void testRoundTripDecisionTable(
            String ruleId,
            Message originalMessage,
            boolean shouldMatch) {
        
        // 编组
        TlvMessage tlvMessage = TlvCodec.marshal(originalMessage);
        assertNotNull(tlvMessage);
        
        // 解码
        Message decodedMessage = TlvCodec.unmarshal(tlvMessage);
        assertNotNull(decodedMessage);
        
        if (shouldMatch) {
            assertEquals(originalMessage.getType(), decodedMessage.getType());
            assertEquals(originalMessage.getImei(), decodedMessage.getImei());
            assertEquals(originalMessage.getImsi(), decodedMessage.getImsi());
            assertEquals(originalMessage.getLcdWidth(), decodedMessage.getLcdWidth());
            assertEquals(originalMessage.getLcdHeight(), decodedMessage.getLcdHeight());
            assertEquals(originalMessage.getAppType(), decodedMessage.getAppType());
        }
    }

    /**
     * 决策表数据源：编解码往返
     */
    static Stream<Arguments> roundTripDecisionTable() {
        return Stream.of(
            // R1: LOGIN消息，字段完整 -> 往返一致
            Arguments.of("R1", createMessage(MessageType.LOGIN, TEST_IMEI, TEST_IMSI), true),
            
            // R2: HEARTBEATS消息，字段完整 -> 往返一致
            Arguments.of("R2", createMessage(MessageType.HEARTBEATS, TEST_IMEI, TEST_IMSI), true),
            
            // R3: LOGOUT消息，字段完整 -> 往返一致
            Arguments.of("R3", createMessage(MessageType.LOGOUT, TEST_IMEI, TEST_IMSI), true),
            
            // R4: 预开浏览器消息（audType和token为空） -> 往返一致
            Arguments.of("R4", createPreOpenMessage(), true)
        );
    }

    /**
     * 测试字节序编组
     */
    @Test
    @DisplayName("字节序编组测试")
    void testMarshalWithByteOrder() {
        Message message = createMessage(MessageType.LOGIN, TEST_IMEI, TEST_IMSI);
        
        // 测试大端序
        byte[] bigEndianBytes = TlvCodec.marshalWithByteOrder(message, ByteOrder.BIG_ENDIAN);
        assertNotNull(bigEndianBytes);
        assertTrue(bigEndianBytes.length > 0);
        
        // 测试小端序
        byte[] littleEndianBytes = TlvCodec.marshalWithByteOrder(message, ByteOrder.LITTLE_ENDIAN);
        assertNotNull(littleEndianBytes);
        assertTrue(littleEndianBytes.length > 0);
        
        // 大端序和小端序的长度应该相同
        assertEquals(bigEndianBytes.length, littleEndianBytes.length);
    }

    /**
     * 测试null消息的字节序编组
     */
    @Test
    @DisplayName("字节序编组异常场景：消息为null")
    void testMarshalWithByteOrderNullMessage() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            TlvCodec.marshalWithByteOrder(null, ByteOrder.BIG_ENDIAN);
        });
        assertTrue(exception.getMessage().contains("消息对象不能为null"));
    }

    /**
     * 测试格式设置
     */
    @Test
    @DisplayName("TLV格式设置测试")
    void testSetFormat() {
        TlvCodec.setFormat(TlvCodec.TlvFormat.BINARY);
        assertEquals(TlvCodec.TlvFormat.BINARY, TlvCodec.getFormat());
        
        TlvCodec.setFormat(TlvCodec.TlvFormat.JSON);
        assertEquals(TlvCodec.TlvFormat.JSON, TlvCodec.getFormat());
    }

    // ========== 辅助方法 ==========

    private static Message createMessage(short type, String imei, String imsi) {
        Message message = new Message();
        message.setType(type);
        message.setImei(imei);
        message.setImsi(imsi);
        message.setLcdWidth(1920);
        message.setLcdHeight(1080);
        message.setAppType("mobile");
        message.setAudType("test");
        message.setToken("test_token");
        return message;
    }

    private static Message createEmptyMessage(short type) {
        Message message = new Message();
        message.setType(type);
        return message;
    }

    private static Message createPreOpenMessage() {
        Message message = new Message();
        message.setType(MessageType.LOGIN);
        message.setImei(TEST_IMEI);
        message.setImsi(TEST_IMSI);
        message.setLcdWidth(1920);
        message.setLcdHeight(1080);
        message.setAppType("mobile");
        // 预开浏览器时，audType和token为空
        message.setAudType("");
        message.setToken("");
        return message;
    }

    private static TlvMessage createTlvMessage(short type, String imei, String imsi) {
        Message message = createMessage(type, imei, imsi);
        return TlvCodec.marshal(message);
    }

    private static TlvMessage createEmptyTlvMessage(short type) {
        Message message = createEmptyMessage(type);
        return TlvCodec.marshal(message);
    }
}
