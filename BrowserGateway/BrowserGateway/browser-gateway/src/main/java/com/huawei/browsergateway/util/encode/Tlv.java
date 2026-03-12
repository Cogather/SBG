package com.huawei.browsergateway.util.encode;

import com.huawei.browsergateway.common.ID;
import lombok.Data;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/** TLV 数据结构，支持序列化/反序列化 */
@Data
public class Tlv {

    /** 协议魔数 */
    public static final short MAGIC = 28021;

    private short magic;
    private int count;
    private int len;
    private List<TlvField> fields = new ArrayList<>();

    /**
     * 从字段列表中查找 type 字段值
     *
     * @return 消息类型值
     * @throws IllegalArgumentException 未找到 type 字段时抛出
     */
    public int getType() {
        return fields.stream()
                .filter(f -> f.getType() == ID.TYPE)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("type field not found"))
                .getInt();
    }

    /**
     * 将 TLV 结构序列化为字节数组
     *
     * @param order 字节序
     * @return 序列化后的字节数组
     * @throws IOException 序列化失败时抛出
     */
    public byte[] marshal(ByteOrder order) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            this.magic = MAGIC;
            writeShort(baos, this.magic, order);
            writeInt(baos, this.count, order);
            writeInt(baos, this.len, order);
            for (TlvField field : fields) {
                writeInt(baos, field.getType(), order);
                writeInt(baos, field.getLen(), order);
                if (field.getData() != null) {
                    baos.write(field.getData());
                }
            }
            return baos.toByteArray();
        }
    }

    // ---- 字节序感知的读写辅助方法 ----

    private void writeShort(ByteArrayOutputStream out, short value, ByteOrder order) throws IOException {
        byte[] bytes = new byte[2];
        ByteBuffer.wrap(bytes).order(order).putShort(value);
        out.write(bytes);
    }

    private void writeInt(ByteArrayOutputStream out, int value, ByteOrder order) throws IOException {
        byte[] bytes = new byte[4];
        ByteBuffer.wrap(bytes).order(order).putInt(value);
        out.write(bytes);
    }

    private short readShort(InputStream in, ByteOrder order) throws IOException {
        byte[] bytes = new byte[2];
        readFully(in, bytes);
        return ByteBuffer.wrap(bytes).order(order).getShort();
    }

    private int readInt(InputStream in, ByteOrder order) throws IOException {
        byte[] bytes = new byte[4];
        readFully(in, bytes);
        return ByteBuffer.wrap(bytes).order(order).getInt();
    }

    /** 确保从流中读满 buffer.length 字节 */
    private void readFully(InputStream in, byte[] buffer) throws IOException {
        int total = 0;
        while (total < buffer.length) {
            int read = in.read(buffer, total, buffer.length - total);
            if (read == -1) break;
            total += read;
        }
        if (total != buffer.length) {
            throw new IOException(String.format(
                    "expected %d bytes but read %d", buffer.length, total));
        }
    }
}
