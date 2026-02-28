package com.huawei.browsergateway.adapter.dto;

/**
 * 证书场景
 */
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
    
    public String getSceneName() {
        return sceneName;
    }
    
    public void setSceneName(String sceneName) {
        this.sceneName = sceneName;
    }
    
    public String getSceneDescCN() {
        return sceneDescCN;
    }
    
    public void setSceneDescCN(String sceneDescCN) {
        this.sceneDescCN = sceneDescCN;
    }
    
    public String getSceneDescEN() {
        return sceneDescEN;
    }
    
    public void setSceneDescEN(String sceneDescEN) {
        this.sceneDescEN = sceneDescEN;
    }
    
    public SceneType getSceneType() {
        return sceneType;
    }
    
    public void setSceneType(SceneType sceneType) {
        this.sceneType = sceneType;
    }
    
    public int getFeature() {
        return feature;
    }
    
    public void setFeature(int feature) {
        this.feature = feature;
    }
}
