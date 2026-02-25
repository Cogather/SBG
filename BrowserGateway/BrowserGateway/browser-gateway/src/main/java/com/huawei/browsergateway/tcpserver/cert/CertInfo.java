package com.huawei.browsergateway.tcpserver.cert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 证书信息单例类
 * 用于存储和管理证书信息，支持线程安全的读写操作
 */
public class CertInfo {
    
    private static final Logger log = LoggerFactory.getLogger(CertInfo.class);
    
    private static volatile CertInfo instance;
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    private String caContent;
    private String deviceContent;
    private String privateKey;
    private long lastUpdateTime;
    
    /**
     * 私有构造函数，防止外部实例化
     */
    private CertInfo() {
        this.caContent = "";
        this.deviceContent = "";
        this.privateKey = "";
        this.lastUpdateTime = 0L;
    }
    
    /**
     * 获取单例实例（双重检查锁定）
     * 
     * @return CertInfo实例
     */
    public static CertInfo getInstance() {
        if (instance == null) {
            synchronized (CertInfo.class) {
                if (instance == null) {
                    instance = new CertInfo();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取CA证书内容
     * 
     * @return CA证书内容
     */
    public String getCaContent() {
        lock.readLock().lock();
        try {
            return caContent;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 设置CA证书内容
     * 
     * @param caContent CA证书内容
     */
    public void setCaContent(String caContent) {
        lock.writeLock().lock();
        try {
            this.caContent = caContent != null ? caContent : "";
            this.lastUpdateTime = System.currentTimeMillis();
            log.debug("CA证书内容已更新，时间戳: {}", this.lastUpdateTime);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取设备证书内容
     * 
     * @return 设备证书内容
     */
    public String getDeviceContent() {
        lock.readLock().lock();
        try {
            return deviceContent;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 设置设备证书内容
     * 
     * @param deviceContent 设备证书内容
     */
    public void setDeviceContent(String deviceContent) {
        lock.writeLock().lock();
        try {
            this.deviceContent = deviceContent != null ? deviceContent : "";
            this.lastUpdateTime = System.currentTimeMillis();
            log.debug("设备证书内容已更新，时间戳: {}", this.lastUpdateTime);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取私钥内容
     * 
     * @return 私钥内容
     */
    public String getPrivateKey() {
        lock.readLock().lock();
        try {
            return privateKey;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 设置私钥内容
     * 
     * @param privateKey 私钥内容
     */
    public void setPrivateKey(String privateKey) {
        lock.writeLock().lock();
        try {
            this.privateKey = privateKey != null ? privateKey : "";
            this.lastUpdateTime = System.currentTimeMillis();
            log.debug("私钥内容已更新，时间戳: {}", this.lastUpdateTime);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取最后更新时间
     * 
     * @return 最后更新时间（毫秒时间戳）
     */
    public long getLastUpdateTime() {
        lock.readLock().lock();
        try {
            return lastUpdateTime;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 检查证书是否就绪
     * 
     * @return 证书是否就绪
     */
    public boolean isReady() {
        lock.readLock().lock();
        try {
            return caContent != null && !caContent.isEmpty() 
                && deviceContent != null && !deviceContent.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 更新所有证书信息
     * 
     * @param caContent CA证书内容
     * @param deviceContent 设备证书内容
     * @param privateKey 私钥内容
     */
    public void updateAll(String caContent, String deviceContent, String privateKey) {
        lock.writeLock().lock();
        try {
            this.caContent = caContent != null ? caContent : "";
            this.deviceContent = deviceContent != null ? deviceContent : "";
            this.privateKey = privateKey != null ? privateKey : "";
            this.lastUpdateTime = System.currentTimeMillis();
            log.info("所有证书信息已更新，时间戳: {}", this.lastUpdateTime);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 清空所有证书信息
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            this.caContent = "";
            this.deviceContent = "";
            this.privateKey = "";
            this.lastUpdateTime = 0L;
            log.info("证书信息已清空");
        } finally {
            lock.writeLock().unlock();
        }
    }
}
