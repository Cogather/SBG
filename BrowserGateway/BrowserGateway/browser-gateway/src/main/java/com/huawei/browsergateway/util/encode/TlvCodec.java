package com.huawei.browsergateway.util.encode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.lang.reflect.Field;
import java.util.Map;

public class TlvCodec {
    /**
     * 将Java对象转换为TLV结构
     * @param obj 要转换的Java对象
     * @return 转换后的TLV结构
     * @throws Exception 转换过程中的异常
     */
    public static Tlv marshal(Object obj) throws Exception {
        // 获取对象的反射信息，处理指针/包装类
        Class<?> clazz = obj.getClass();
        if (clazz.isPrimitive() || isWrapperType(clazz)) {
            throw new IllegalArgumentException("不支持基本类型，需要传入对象");
        }

        // 初始化TLV
        Tlv tlv = new Tlv();

        // 遍历所有字段
        List<TlvField> tlvFields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            // 检查是否有TLV标签
            TlvTag tag = field.getAnnotation(TlvTag.class);
            if (tag == null) {
                continue;
            }

            // 允许访问私有字段
            field.setAccessible(true);

            // 转换字段值为字节数组
            byte[] data = fieldToBytes(field.get(obj), tag.type(), field.getName());

            // 添加到TLV字段列表
            tlvFields.add(new TlvField(tag.id(), data.length, data));
        }

        // 设置TLV的计数和长度
        tlv.setCount(tlvFields.size());
        tlv.setFields(tlvFields);

        // 计算总长度
        int totalLen = 0;
        for (TlvField f : tlvFields) {
            totalLen += 8 + f.getLen(); // Type(4) + Len(4) + Data
        }
        tlv.setLen(totalLen);

        return tlv;
    }

    /**
     * 将TLV结构解析到Java对象中
     * @param tlv TLV结构
     * @param obj 要填充的Java对象
     * @throws Exception 解析过程中的异常
     */
    public static void unmarshal(Tlv tlv, Object obj) throws Exception {
        // 验证输入
        if (obj == null) {
            throw new IllegalArgumentException("目标对象不能为null");
        }

        Class<?> clazz = obj.getClass();

        // 按Type构建字段映射，加速查找
        Map<Integer, TlvField> fieldsByType = new HashMap<>();
        for (TlvField field : tlv.getFields()) {
            fieldsByType.put(field.getType(), field);
        }

        // 遍历对象字段
        for (Field field : clazz.getDeclaredFields()) {
            // 检查是否有TLV标签
            TlvTag tag = field.getAnnotation(TlvTag.class);
            if (tag == null) {
                continue;
            }

            // 查找对应的TLV字段
            TlvField tlvField = fieldsByType.get(tag.id());
            if (tlvField == null) {
                continue;
            }

            // 允许访问私有字段
            field.setAccessible(true);

            // 设置字段值
            setFieldValue(obj, field, tlvField.getData(), tag.type());
        }
    }

    // ============================ 辅助方法 =============================
    /**
     * 将字段值转换为字节数组
     */
    private static byte[] fieldToBytes(Object value, String fieldType, String fieldName) throws Exception {
        if (value == null) {
            return new byte[0];
        }

        switch (fieldType) {
            case "string":
                if (!(value instanceof String)) {
                    throw new IllegalArgumentException("字段" + fieldName + "标记为string，但实际类型是"
                            + value.getClass().getSimpleName());
                }
                return ((String) value).getBytes();

            case "int32":
                if (!(value instanceof Integer)) {
                    throw new IllegalArgumentException("字段" + fieldName + "标记为int32，但实际类型是"
                            + value.getClass().getSimpleName());
                }
                ByteBuffer buffer = ByteBuffer.allocate(4);
                buffer.order(ByteOrder.BIG_ENDIAN);
                buffer.putInt((Integer) value);
                return buffer.array();

            case "bytes":
                if (!(value instanceof byte[])) {
                    throw new IllegalArgumentException("字段" + fieldName + "标记为bytes，但实际类型是"
                            + value.getClass().getSimpleName());
                }
                // 确保是byte[]类型后再克隆
                byte[] original = (byte[]) value;
                return original.clone();

            case "int64":
                if (!(value instanceof Long)) {
                    throw new IllegalArgumentException("字段" + fieldName + "标记为int64，但实际类型是"
                            + value.getClass().getSimpleName());
                }
                ByteBuffer bufferLong = ByteBuffer.allocate(8);
                bufferLong.order(ByteOrder.BIG_ENDIAN);
                bufferLong.putLong((Long) value);
                return bufferLong.array();

            default:
                throw new IllegalArgumentException("字段" + fieldName + "不支持的类型" + fieldType);
        }
    }

    /**
     * 根据字节数组设置字段值
     */
    private static void setFieldValue(Object obj, Field field, byte[] data, String fieldType) throws Exception {
        switch (fieldType) {
            case "string":
                field.set(obj, new String(data));
                break;

            case "int32":
                if (data.length != 4) {
                    throw new IllegalArgumentException("字段" + field.getName() + "的int32数据长度无效：" + data.length);
                }
                ByteBuffer buffer = ByteBuffer.wrap(data);
                buffer.order(ByteOrder.BIG_ENDIAN);
                field.set(obj, buffer.getInt());
                break;

            case "bytes":
                byte[] copy = new byte[data.length];
                System.arraycopy(data, 0, copy, 0, data.length);
                field.set(obj, copy);
                break;

            case "int64":
                if (data.length != 8) {
                    throw new IllegalArgumentException("字段" + field.getName() + "的int64数据长度无效：" + data.length);
                }
                ByteBuffer bufferLong = ByteBuffer.wrap(data);
                bufferLong.order(ByteOrder.BIG_ENDIAN);
                field.set(obj, bufferLong.getLong());
                break;

            default:
                throw new IllegalArgumentException("字段" + field.getName() + "不支持的类型" + fieldType);
        }
    }

    /**
     * 检查是否为包装类型
     */
    private static boolean isWrapperType(Class<?> clazz) {
        return clazz == Integer.class || clazz == Long.class || clazz == Short.class ||
                clazz == Byte.class || clazz == Boolean.class || clazz == Character.class ||
                clazz == Float.class || clazz == Double.class;
    }
}