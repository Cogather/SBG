package com.huawei.browsergateway.service.storage;

import com.huawei.browsergateway.common.Constants;
import com.huawei.browsergateway.exception.storage.FileStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 存储策略选择器
 */
public class StorageStrategySelector {
    
    private static final Logger log = LoggerFactory.getLogger(StorageStrategySelector.class);
    
    private final List<StorageAdapter> adapters = new ArrayList<>();
    private final Map<String, StorageAdapter> typeAdapters = new HashMap<>();
    
    /**
     * 注册存储适配器
     * 
     * @param type 存储类型
     * @param adapter 存储适配器
     */
    public void registerAdapter(StorageType type, StorageAdapter adapter) {
        if (adapter == null) {
            throw new IllegalArgumentException("存储适配器不能为空");
        }
        adapters.add(adapter);
        typeAdapters.put(type.name(), adapter);
        log.info("注册存储适配器: type={}", type);
    }
    
    /**
     * 根据配置选择适配器
     * 
     * @param config 存储配置
     * @return 存储适配器
     * @throws FileStorageException 选择失败时抛出
     */
    public StorageAdapter selectAdapter(StorageConfig config) throws FileStorageException {
        if (config == null) {
            throw new IllegalArgumentException("存储配置不能为空");
        }
        
        StorageType storageType = config.getStorageType();
        if (storageType == null) {
            throw new FileStorageException("存储类型不能为空");
        }
        
        StorageAdapter adapter = typeAdapters.get(storageType.name());
        if (adapter == null) {
            throw new FileStorageException("未找到存储类型对应的适配器: " + storageType);
        }
        
        // 初始化适配器
        try {
            adapter.init(config);
            return adapter;
        } catch (Exception e) {
            throw new FileStorageException("初始化存储适配器失败: " + storageType, e);
        }
    }
    
    /**
     * 根据文件路径选择适配器
     * 
     * @param path 文件路径
     * @return 存储适配器
     */
    public StorageAdapter selectAdapterByPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return adapters.isEmpty() ? null : adapters.get(0);
        }
        
        // user/或cache/开头的路径使用本地存储适配器
        if (path.startsWith(Constants.USER_DATA_PATH_PREFIX) || 
            path.startsWith(Constants.CACHE_PATH_PREFIX)) {
            StorageAdapter localAdapter = typeAdapters.get(StorageType.LOCAL.name());
            if (localAdapter != null) {
                return localAdapter;
            }
        }
        
        // 其他路径使用第一个适配器
        return adapters.isEmpty() ? null : adapters.get(0);
    }
    
    /**
     * 获取所有已注册的适配器
     */
    public List<StorageAdapter> getAllAdapters() {
        return new ArrayList<>(adapters);
    }
}
