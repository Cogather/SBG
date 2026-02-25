package com.huawei.browsergateway.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户数据脱敏工具类
 * 用于在上传用户数据前移除敏感信息
 */
public class UserdataSlimmer {
    
    private static final Logger log = LoggerFactory.getLogger(UserdataSlimmer.class);
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 敏感字段集合（需要移除的字段名）
     */
    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>();
    
    static {
        // 密码相关字段
        SENSITIVE_FIELDS.add("password");
        SENSITIVE_FIELDS.add("passwd");
        SENSITIVE_FIELDS.add("pwd");
        SENSITIVE_FIELDS.add("pass");
        
        // 认证相关字段
        SENSITIVE_FIELDS.add("token");
        SENSITIVE_FIELDS.add("accessToken");
        SENSITIVE_FIELDS.add("refreshToken");
        SENSITIVE_FIELDS.add("authToken");
        SENSITIVE_FIELDS.add("apiKey");
        SENSITIVE_FIELDS.add("secretKey");
        SENSITIVE_FIELDS.add("secret");
        
        // 个人信息相关字段
        SENSITIVE_FIELDS.add("creditCard");
        SENSITIVE_FIELDS.add("cardNumber");
        SENSITIVE_FIELDS.add("cvv");
        SENSITIVE_FIELDS.add("ssn");
        SENSITIVE_FIELDS.add("socialSecurityNumber");
        
        // 会话相关字段
        SENSITIVE_FIELDS.add("sessionId");
        SENSITIVE_FIELDS.add("sessionKey");
        SENSITIVE_FIELDS.add("cookie");
        SENSITIVE_FIELDS.add("cookies");
        
        // 其他敏感字段
        SENSITIVE_FIELDS.add("privateKey");
        SENSITIVE_FIELDS.add("private_key");
        SENSITIVE_FIELDS.add("privateKeyData");
    }
    
    /**
     * 对用户数据文件进行脱敏处理（原地修改）
     * 
     * @param userDataFile 用户数据文件（JSON格式）
     * @throws IOException 文件读写异常
     */
    public static void slimInplace(File userDataFile) throws IOException {
        if (userDataFile == null || !userDataFile.exists()) {
            throw new IOException("用户数据文件不存在: " + userDataFile);
        }
        
        if (!userDataFile.isFile()) {
            throw new IOException("路径不是文件: " + userDataFile);
        }
        
        log.info("开始对用户数据进行脱敏处理: {}", userDataFile.getAbsolutePath());
        
        try {
            // 读取JSON文件
            JsonNode rootNode = objectMapper.readTree(userDataFile);
            
            // 脱敏处理
            JsonNode slimmedNode = slimJsonNode(rootNode);
            
            // 写回文件
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(userDataFile, slimmedNode);
            
            log.info("用户数据脱敏完成: {}", userDataFile.getAbsolutePath());
            
        } catch (IOException e) {
            log.error("用户数据脱敏失败: {}", userDataFile.getAbsolutePath(), e);
            throw new IOException("用户数据脱敏失败: " + userDataFile.getAbsolutePath(), e);
        }
    }
    
    /**
     * 对用户数据文件进行脱敏处理（原地修改）
     * 
     * @param userDataPath 用户数据文件路径（JSON格式）
     * @throws IOException 文件读写异常
     */
    public static void slimInplace(String userDataPath) throws IOException {
        if (userDataPath == null || userDataPath.trim().isEmpty()) {
            throw new IllegalArgumentException("用户数据路径不能为空");
        }
        
        Path path = Paths.get(userDataPath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IOException("用户数据文件不存在: " + userDataPath);
        }
        
        slimInplace(path.toFile());
    }
    
    /**
     * 对用户数据目录进行脱敏处理
     * 处理目录下的所有JSON文件
     * 
     * @param userDataDir 用户数据目录
     * @throws IOException 文件读写异常
     */
    public static void slimDirectory(File userDataDir) throws IOException {
        if (userDataDir == null || !userDataDir.exists()) {
            throw new IOException("用户数据目录不存在: " + userDataDir);
        }
        
        if (!userDataDir.isDirectory()) {
            throw new IOException("路径不是目录: " + userDataDir);
        }
        
        log.info("开始对用户数据目录进行脱敏处理: {}", userDataDir.getAbsolutePath());
        
        File[] files = userDataDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    slimInplace(file);
                } catch (IOException e) {
                    log.warn("脱敏处理文件失败，跳过: {}", file.getAbsolutePath(), e);
                }
            }
        }
        
        log.info("用户数据目录脱敏完成: {}", userDataDir.getAbsolutePath());
    }
    
    /**
     * 递归处理JSON节点，移除敏感字段
     * 
     * @param node JSON节点
     * @return 脱敏后的JSON节点
     */
    private static JsonNode slimJsonNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        
        if (node.isObject()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            
            node.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode fieldValue = entry.getValue();
                
                // 检查是否为敏感字段
                if (isSensitiveField(fieldName)) {
                    log.debug("移除敏感字段: {}", fieldName);
                    // 跳过敏感字段，不添加到结果中
                } else {
                    // 递归处理子节点
                    JsonNode slimmedValue = slimJsonNode(fieldValue);
                    objectNode.set(fieldName, slimmedValue);
                }
            });
            
            return objectNode;
            
        } else if (node.isArray()) {
            // 处理数组节点
            com.fasterxml.jackson.databind.node.ArrayNode arrayNode = objectMapper.createArrayNode();
            for (JsonNode element : node) {
                arrayNode.add(slimJsonNode(element));
            }
            return arrayNode;
            
        } else {
            // 基本类型（字符串、数字、布尔值等）直接返回
            return node;
        }
    }
    
    /**
     * 检查字段名是否为敏感字段
     * 
     * @param fieldName 字段名
     * @return 是否为敏感字段
     */
    private static boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        
        String lowerFieldName = fieldName.toLowerCase();
        
        // 精确匹配
        if (SENSITIVE_FIELDS.contains(lowerFieldName)) {
            return true;
        }
        
        // 模糊匹配（包含敏感关键词）
        for (String sensitiveKeyword : SENSITIVE_FIELDS) {
            if (lowerFieldName.contains(sensitiveKeyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 验证用户数据是否已脱敏
     * 
     * @param userDataFile 用户数据文件
     * @return 是否已脱敏（不包含敏感字段）
     */
    public static boolean isSlimmed(File userDataFile) {
        if (userDataFile == null || !userDataFile.exists()) {
            return false;
        }
        
        try {
            JsonNode rootNode = objectMapper.readTree(userDataFile);
            return !containsSensitiveFields(rootNode);
        } catch (IOException e) {
            log.error("验证用户数据脱敏状态失败", e);
            return false;
        }
    }
    
    /**
     * 检查JSON节点是否包含敏感字段
     * 
     * @param node JSON节点
     * @return 是否包含敏感字段
     */
    private static boolean containsSensitiveFields(JsonNode node) {
        if (node == null || node.isNull()) {
            return false;
        }
        
        if (node.isObject()) {
            java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                java.util.Map.Entry<String, JsonNode> entry = fields.next();
                String fieldName = entry.getKey();
                JsonNode fieldValue = entry.getValue();
                if (isSensitiveField(fieldName) || containsSensitiveFields(fieldValue)) {
                    return true;
                }
            }
            return false;
        } else if (node.isArray()) {
            return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .anyMatch(UserdataSlimmer::containsSensitiveFields);
        }
        
        return false;
    }
}
