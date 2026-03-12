package com.huawei.browsergateway.util.encode;

import com.huawei.browsergateway.common.Type;
import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for TlvCodec and Tlv serialization */
class TlvCodecTest {

    static class SampleMsg {
        @TlvTag(type = "int32", id = 1)
        private int type;

        @TlvTag(type = "string", id = 2)
        private String name;

        @TlvTag(type = "bytes", id = 3)
        private byte[] data;
    }

    @Test
    void testMarshal_fieldCountAndLength() throws Exception {
        SampleMsg msg = new SampleMsg();
        msg.type = 7;
        msg.name = "hello";
        msg.data = new byte[]{1, 2, 3};

        Tlv tlv = TlvCodec.marshal(msg);

        assertEquals(3, tlv.getCount());
        // (4+4+4) + (4+4+5) + (4+4+3) = 36
        assertEquals(36, tlv.getLen());
    }

    @Test
    void testMarshalAndUnmarshal_roundTrip() throws Exception {
        SampleMsg original = new SampleMsg();
        original.type = 42;
        original.name = "world";
        original.data = new byte[]{10, 20, 30};

        Tlv tlv = TlvCodec.marshal(original);

        SampleMsg restored = new SampleMsg();
        TlvCodec.unmarshal(tlv, restored);

        assertEquals(original.type, restored.type);
        assertEquals(original.name, restored.name);
        assertArrayEquals(original.data, restored.data);
    }

    @Test
    void testMarshal_primitiveTypeThrows() {
        assertThrows(IllegalArgumentException.class, () -> TlvCodec.marshal(42));
    }

    @Test
    void testUnmarshal_nullTargetThrows() throws Exception {
        SampleMsg msg = new SampleMsg();
        Tlv tlv = TlvCodec.marshal(msg);
        assertThrows(IllegalArgumentException.class, () -> TlvCodec.unmarshal(tlv, null));
    }

    @Test
    void testTlvMarshal_magicBytesCorrect() throws Exception {
        SampleMsg msg = new SampleMsg();
        msg.type = 1;
        msg.name = "ab";
        msg.data = new byte[0];

        byte[] bytes = TlvCodec.marshal(msg).marshal(ByteOrder.BIG_ENDIAN);

        assertNotNull(bytes);
        short magic = (short) (((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF));
        assertEquals(Tlv.MAGIC, magic);
    }

    @Test
    void testAck_encodesWithoutError() throws Exception {
        Ack ack = new Ack(Type.ACK, 200);
        byte[] bytes = TlvCodec.marshal(ack).marshal(ByteOrder.BIG_ENDIAN);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }
}
