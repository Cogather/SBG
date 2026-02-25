package com.huawei.browsergateway.adapter.dto;

import lombok.Data;

/**
 * 证书场景
 */
@Data
public class CertScene {
    private String sceneName;
    private String sceneDescCN;
    private String sceneDescEN;
    private SceneType sceneType;
    private int feature;
    
    public enum SceneType {
        CA,
        DEVICE
    }
}
