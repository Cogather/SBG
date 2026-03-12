package com.huawei.browsergateway.util.encode;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** TLV 编解码工具类，通过反射将 Java 对象与 TLV 格式互转 */
public final class TlvCodec {

    private TlvCodec() {}

    /**
     * 将 Java 对象序列化为 TLV 结构
     *
     * @param obj 待序列化对象（不支持基本类型/包装类）
     * @return TLV 结构
     * @throws Exception 字段类型不匹配或反射异常时抛出
     */
    public static Tlv marshal(Object obj) throws Exception {
        Class<?> clazz = obj.getClass();
        if (clazz.isPrimitive() || isWrapperType(clazz)) {
            throw new IllegalArgumentException("不支持基本类型，需要传入对象");
        }

        List<TlvField> tlvFields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            TlvTag tag = field.getAnnotation(TlvTag.class);
            if (tag == null) continue;
            field.setAccessible(true);
            byte[] data = toBytes(field.get(obj), tag.type(), field.getName());
            tlvFields.add(new TlvField(tag.id(), data.length, data));
        }

        int totalLen = tlvFields.stream().mapToInt(f -> 8 + f.getLen()).sum();

        Tlv tlv = new Tlv();
        tlv.setCount(tlvFields.size());
        tlv.setFields(tlvFields);
        tlv.setLen(totalLen);
        return tlv;
    }

    /**
     * 将 TLV 结构反序列化到 Java 对象
     *
     * @param tlv TLV 结构
     * @param obj 目标对象（不能为 null）
     * @throws Exception 字段类型不匹配或反射异常时抛出
     */
    public static void unmarshal(Tlv tlv, Object obj) throws Exception {
        if (obj == null) throw new IllegalArgumentException("目标对象不能为 null");

        // 按 type 建立索引，加速查找
        Map<Integer, TlvField> byType = new HashMap<>();
        for (TlvField f : tlv.getFields()) {
            byType.put(f.getType(), f);
        }

        for (Field field : obj.getClass().getDeclaredFields()) {
            TlvTag tag = field.getAnnotation(TlvTag.class);
            if (tag == null) continue;
            TlvField tlvField = byType.get(tag.id());
            if (tlvField == null) continue;
            field.setAccessible(true);
            fromBytes(obj, field, tlvField.getData(), tag.type());
        }
    }

    // ---- 字段值 <-> 字节数组 转换 ----

    /** 将字段值转换为字节数组 */
    private static byte[] toBytes(Object value, String type, String fieldName) throws Exception {
        if (value == null) return new byte[0];
        switch (type) {
            case "string":
                assertType(value, String.class, type, fieldName);
                return ((String) value).getBytes();
            case "int32":
                assertType(value, Integer.class, type, fieldName);
                return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt((Integer) value).array();
            case "int64":
                assertType(value, Long.class, type, fieldName);
                return ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong((Long) value).array();
            case "bytes":
                assertType(value, byte[].class, type, fieldName);
                return ((byte[]) value).clone();
            default:
                throw new IllegalArgumentException("字段 " + fieldName + " 不支持的类型: " + type);
        }
    }

    /** 将字节数组写入对象字段 */
    private static void fromBytes(Object obj, Field field, byte[] data, String type) throws Exception {
        switch (type) {
            case "string":
                field.set(obj, new String(data));
                break;
            case "int32":
                if (data.length != 4) throw new IllegalArgumentException(
                        "字段 " + field.getName() + " int32 数据长度无效: " + data.length);
                field.set(obj, ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getInt());
                break;
            case "int64":
                if (data.length != 8) throw new IllegalArgumentException(
                        "字段 " + field.getName() + " int64 数据长度无效: " + data.length);
                field.set(obj, ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getLong());
                break;
            case "bytes":
                byte[] copy = new byte[data.length];
                System.arraycopy(data, 0, copy, 0, data.length);
                field.set(obj, copy);
                break;
            default:
                throw new IllegalArgumentException("字段 " + field.getName() + " 不支持的类型: " + type);
        }
    }

    private static void assertType(Object value, Class<?> expected, String type, String fieldName) {
        if (!expected.isInstance(value)) {
            throw new IllegalArgumentException("字段 " + fieldName + " 标记为 " + type
                    + "，但实际类型是 " + value.getClass().getSimpleName());
        }
    }

    private static boolean isWrapperType(Class<?> clazz) {
        return clazz == Integer.class || clazz == Long.class || clazz == Short.class
                || clazz == Byte.class || clazz == Boolean.class || clazz == Character.class
                || clazz == Float.class || clazz == Double.class;
    }
}
