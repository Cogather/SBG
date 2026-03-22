package com.huawei.mobile.dto;

public class UseTimesEvent {
    private Long useTimes;
    private String hsman;
    private String hstype;
    private String appType;
    private String appId;
    private Integer scheight;
    private Integer scwidth;
    private String exttype;
    private String imei;
    private String imsi;
    private Integer playMode;

    public Long getUseTimes() { return useTimes; }
    public void setUseTimes(Long useTimes) { this.useTimes = useTimes; }
    public String getHsman() { return hsman; }
    public void setHsman(String hsman) { this.hsman = hsman; }
    public String getHstype() { return hstype; }
    public void setHstype(String hstype) { this.hstype = hstype; }
    public String getAppType() { return appType; }
    public void setAppType(String appType) { this.appType = appType; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public Integer getScheight() { return scheight; }
    public void setScheight(Integer scheight) { this.scheight = scheight; }
    public Integer getScwidth() { return scwidth; }
    public void setScwidth(Integer scwidth) { this.scwidth = scwidth; }
    public String getExttype() { return exttype; }
    public void setExttype(String exttype) { this.exttype = exttype; }
    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }
    public String getImsi() { return imsi; }
    public void setImsi(String imsi) { this.imsi = imsi; }
    public Integer getPlayMode() { return playMode; }
    public void setPlayMode(Integer playMode) { this.playMode = playMode; }
}
