package com.huawei.browsergateway.util.encode;

import com.huawei.browsergateway.common.ID;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * TLV数据结构
 */
@Data
public class Tlv {
    public static final short MAGIC = 28021; // 魔数
    private static final int HEADER = 1;      // 头部类型


    private short magic;    // 魔数
    private int count;      // 字段个数
    private int len;        // 总长度
    private List<TlvField> fields = new ArrayList<>();
    private int type;       // 请求数据类型，内部字段

    public void setMagic(short magic) {
        this.magic = magic;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public void setFields(List<TlvField> fields) {
        this.fields = fields;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        for (TlvField field : fields) {
            if (field.getType() == ID.TYPE) {
                return field.getInt();
            }
        }
        throw new IllegalArgumentException("type is not found");
    }

    /**
     * 序列化TLV结构为字节数组
     *
     * @param order 字节序
     * @return 序列化后的字节数组
     * @throws IOException 序列化异常
     */
    public byte[] marshal(ByteOrder order) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            this.magic = MAGIC;

            // 写入魔数
            writeShort(baos, this.magic, order);
            // 写入字段数量
            writeInt(baos, this.count, order);
            // 写入总长度
            writeInt(baos, this.len, order);

            // 写入每个字段
            for (TlvField field : fields) {
                writeInt(baos, field.getType(), order);
                writeInt(baos, field.getLen(), order);
                writeAndCheckSize(baos, field.getData());
            }

            return baos.toByteArray();
        }
    }

    /**
     * 写入short值并保持指定字节序
     */
    private void writeShort(ByteArrayOutputStream out, short value, ByteOrder order) throws IOException {
        byte[] bytes = new byte[2];
        ByteBuffer.wrap(bytes).order(order).putShort(value);
        out.write(bytes);
    }

    /**
     * 写入int值并保持指定字节序
     */
    private void writeInt(ByteArrayOutputStream out, int value, ByteOrder order) throws IOException {
        byte[] bytes = new byte[4];
        ByteBuffer.wrap(bytes).order(order).putInt(value);
        out.write(bytes);
    }

    /**
     * 读取short值并保持指定字节序
     */
    private short readShort(InputStream in, ByteOrder order) throws IOException {
        byte[] bytes = new byte[2];
        readAndCheckSize(in, bytes);
        return ByteBuffer.wrap(bytes).order(order).getShort();
    }

    /**
     * 读取int值并保持指定字节序
     */
    private int readInt(InputStream in, ByteOrder order) throws IOException {
        byte[] bytes = new byte[4];
        readAndCheckSize(in, bytes);
        return ByteBuffer.wrap(bytes).order(order).getInt();
    }

    /**
     * 写入数据并检查写入长度
     */
    private void writeAndCheckSize(ByteArrayOutputStream out, byte[] data) throws IOException {
        if (data == null) {
            out.write(new byte[0]);
            return;
        }

        out.write(data);
    }

    /**
     * 读取数据并检查读取长度
     */
    private void readAndCheckSize(InputStream in, byte[] buffer) throws IOException {
        int totalRead = 0;
        while (totalRead < buffer.length) {
            int read = in.read(buffer, totalRead, buffer.length - totalRead);
            if (read == -1) {
                break;
            }
            totalRead += read;
        }

        if (totalRead != buffer.length) {
            throw new IOException(String.format("data length is: %d, but read size: %d", buffer.length, totalRead));
        }
    }
}