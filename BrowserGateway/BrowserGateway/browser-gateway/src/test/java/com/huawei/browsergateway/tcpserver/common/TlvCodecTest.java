package com.huawei.browsergateway.tcpserver.common;

import com.huawei.browsergateway.common.ID;
import com.huawei.browsergateway.util.encode.Message;
import com.huawei.browsergateway.util.encode.Tlv;
import com.huawei.browsergateway.util.encode.TlvCodec;
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
 * TLV编解码测试类
 * 采用决策表（Decision Table）方法设计测试用例
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TLV编解码测试")
class TlvCodecTest {

    /**
     * 决策表：TLV编组测试
     * 
     * 条件（Conditions）：
     * C1: Message是否为null
     * C2: Message类型是否有效
     * C3: Message字段是否完整
     * 
     * 动作（Actions）：
     * A1: 是否抛出异常
     * A2: TlvMessage是否创建成功
     * A3: TlvMessage类型是否正确
     */
    @ParameterizedTest(name = "TLV编组测试 - 规则{index}: {0}")
    @MethodSource("marshalTestCases")
    @DisplayName("TLV编组测试")
    void testMarshal(
            String description,
            Message message,
            boolean shouldThrowException,
            boolean shouldSucceed
    ) {
        if (shouldThrowException) {
            assertThrows(Exception.class, () -> {
                TlvCodec.marshal(message);
            });
        } else {
            try {
                Tlv tlv = TlvCodec.marshal(message);
                assertNotNull(tlv);
                assertEquals(shouldSucceed, tlv.getCount() > 0);
            } catch (Exception e) {
                fail("编组失败: " + e.getMessage());
            }
        }
    }

    static Stream<Arguments> marshalTestCases() {
        return Stream.of(
            // R1: Message为null → 抛出异常
            Arguments.of(
                "R1: Message为null",
                null,
                true,
                false
            ),
            // R2: Message类型为LOGIN，字段完整 → 成功编组
            Arguments.of(
                "R2: LOGIN消息，字段完整",
                createLoginMessage(),
                false,
                true
            ),
            // R3: Message类型为HEARTBEATS，字段完整 → 成功编组
            Arguments.of(
                "R3: HEARTBEATS消息，字段完整",
                createHeartbeatsMessage(),
                false,
                true
            ),
            // R4: Message类型为LOGOUT，字段完整 → 成功编组
            Arguments.of(
                "R4: LOGOUT消息，字段完整",
                createLogoutMessage(),
                false,
                true
            ),
            // R5: Message字段为空 → 成功编组（空值）
            Arguments.of(
                "R5: Message字段为空",
                createEmptyMessage(),
                false,
                true
            )
        );
    }

    /**
     * 决策表：TLV解码测试
     * 
     * 条件（Conditions）：
     * C1: TlvMessage是否为null
     * C2: TlvMessage类型是否有效
     * C3: TlvMessage值是否为空
     * 
     * 动作（Actions）：
     * A1: 是否抛出异常
     * A2: Message是否创建成功
     * A3: Message类型是否正确
     */
    @ParameterizedTest(name = "TLV解码测试 - 规则{index}: {0}")
    @MethodSource("unmarshalTestCases")
    @DisplayName("TLV解码测试")
    void testUnmarshal(
            String description,
            Tlv tlv,
            Message expectedMessage,
            boolean shouldThrowException
    ) {
        if (shouldThrowException) {
            assertThrows(Exception.class, () -> {
                Message message = new Message();
                TlvCodec.unmarshal(tlv, message);
            });
        } else {
            try {
                Message message = new Message();
                TlvCodec.unmarshal(tlv, message);
                assertNotNull(message);
                if (expectedMessage != null) {
                    assertEquals(expectedMessage.getType(), message.getType());
                }
            } catch (Exception e) {
                fail("解码失败: " + e.getMessage());
            }
        }
    }

    static Stream<Arguments> unmarshalTestCases() {
        return Stream.of(
            // R1: TlvMessage为null → 抛出异常
            Arguments.of(
                "R1: TlvMessage为null",
                null,
                null,
                true
            ),
            // R2: TlvMessage类型为LOGIN，值有效 → 成功解码
            Arguments.of(
                "R2: LOGIN消息，值有效",
                createTlvFromMessage(createLoginMessage()),
                createLoginMessage(),
                false
            ),
            // R3: TlvMessage类型为HEARTBEATS，值有效 → 成功解码
            Arguments.of(
                "R3: HEARTBEATS消息，值有效",
                createTlvFromMessage(createHeartbeatsMessage()),
                createHeartbeatsMessage(),
                false
            ),
            // R4: TlvMessage值为空 → 成功解码（空字段）
            Arguments.of(
                "R4: TlvMessage值为空",
                createTlvFromMessage(createEmptyMessage()),
                createEmptyMessage(),
                false
            )
        );
    }

    /**
     * 决策表：编解码往返测试
     * 
     * 条件（Conditions）：
     * C1: 原始Message类型
     * C2: 原始Message字段
     * 
     * 动作（Actions）：
     * A1: 编解码后Message是否与原始一致
     */
    @ParameterizedTest(name = "编解码往返测试 - 规则{index}: {0}")
    @MethodSource("roundTripTestCases")
    @DisplayName("编解码往返测试")
    void testRoundTrip(
            String description,
            Message originalMessage
    ) {
        try {
            // 编组
            Tlv tlv = TlvCodec.marshal(originalMessage);
            assertNotNull(tlv);

            // 解码
            Message decodedMessage = new Message();
            TlvCodec.unmarshal(tlv, decodedMessage);

            // 验证
            assertEquals(originalMessage.getType(), decodedMessage.getType());
            assertEquals(originalMessage.getImei(), decodedMessage.getImei());
            assertEquals(originalMessage.getImsi(), decodedMessage.getImsi());
            assertEquals(originalMessage.getLcdWidth(), decodedMessage.getLcdWidth());
            assertEquals(originalMessage.getLcdHeight(), decodedMessage.getLcdHeight());
        } catch (Exception e) {
            fail("往返测试失败: " + e.getMessage());
        }
    }

    static Stream<Arguments> roundTripTestCases() {
        return Stream.of(
            // R1: LOGIN消息，字段完整 → 往返一致
            Arguments.of(
                "R1: LOGIN消息，字段完整",
                createLoginMessage()
            ),
            // R2: HEARTBEATS消息，字段完整 → 往返一致
            Arguments.of(
                "R2: HEARTBEATS消息，字段完整",
                createHeartbeatsMessage()
            ),
            // R3: LOGOUT消息，字段完整 → 往返一致
            Arguments.of(
                "R3: LOGOUT消息，字段完整",
                createLogoutMessage()
            ),
            // R4: 预开浏览器消息（audType和token为空） → 往返一致
            Arguments.of(
                "R4: 预开浏览器消息",
                createPreOpenMessage()
            )
        );
    }

    /**
     * 字节序编组测试（大端序/小端序）
     */
    @Test
    @DisplayName("字节序编组测试")
    void testByteOrder() {
        try {
            Message message = createLoginMessage();
            Tlv tlv = TlvCodec.marshal(message);

            // 测试大端序
            byte[] bigEndian = tlv.marshal(ByteOrder.BIG_ENDIAN);
            assertNotNull(bigEndian);
            assertTrue(bigEndian.length > 0);

            // 测试小端序
            byte[] littleEndian = tlv.marshal(ByteOrder.LITTLE_ENDIAN);
            assertNotNull(littleEndian);
            assertTrue(littleEndian.length > 0);

            // 大端序和小端序的长度应该相同
            assertEquals(bigEndian.length, littleEndian.length);
        } catch (Exception e) {
            fail("字节序测试失败: " + e.getMessage());
        }
    }

    /**
     * TLV格式设置测试
     */
    @Test
    @DisplayName("TLV格式设置测试")
    void testTlvFormat() {
        try {
            Message message = createLoginMessage();
            Tlv tlv = TlvCodec.marshal(message);

            // 验证TLV结构
            assertNotNull(tlv);
            assertTrue(tlv.getCount() > 0);
            assertTrue(tlv.getLen() > 0);
            assertNotNull(tlv.getFields());
            assertEquals(tlv.getCount(), tlv.getFields().size());
        } catch (Exception e) {
            fail("TLV格式测试失败: " + e.getMessage());
        }
    }

    /**
     * 辅助方法：创建LOGIN消息
     */
    private static Message createLoginMessage() {
        Message message = new Message();
        message.setType(1); // LOGIN类型
        message.setFactory("test-factory");
        message.setDevType("test-device");
        message.setImsi("123456789012345");
        message.setImei("987654321098765");
        message.setLcdWidth(240);
        message.setLcdHeight(320);
        message.setAudType("test-audio");
        message.setToken("test-token");
        return message;
    }

    /**
     * 辅助方法：创建HEARTBEATS消息
     */
    private static Message createHeartbeatsMessage() {
        Message message = new Message();
        message.setType(2); // HEARTBEATS类型
        message.setImsi("123456789012345");
        message.setImei("987654321098765");
        return message;
    }

    /**
     * 辅助方法：创建LOGOUT消息
     */
    private static Message createLogoutMessage() {
        Message message = new Message();
        message.setType(3); // LOGOUT类型
        message.setImsi("123456789012345");
        message.setImei("987654321098765");
        return message;
    }

    /**
     * 辅助方法：创建空消息
     */
    private static Message createEmptyMessage() {
        Message message = new Message();
        message.setType(1); // 设置类型，其他字段为空
        // 其他字段为空
        return message;
    }

    /**
     * 辅助方法：创建预开浏览器消息
     */
    private static Message createPreOpenMessage() {
        Message message = new Message();
        message.setType(1); // LOGIN类型
        message.setFactory("test-factory");
        message.setDevType("test-device");
        message.setImsi("123456789012345");
        message.setImei("987654321098765");
        message.setLcdWidth(240);
        message.setLcdHeight(320);
        message.setAudType(""); // 空值
        message.setToken(""); // 空值
        return message;
    }

    /**
     * 辅助方法：从Message创建Tlv
     */
    private static Tlv createTlvFromMessage(Message message) {
        try {
            return TlvCodec.marshal(message);
        } catch (Exception e) {
            return null;
        }
    }
}
