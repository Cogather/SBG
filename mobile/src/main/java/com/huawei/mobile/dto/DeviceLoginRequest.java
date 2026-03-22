package com.huawei.mobile.dto;

public class DeviceLoginRequest {
    private String imsi;
    private String imei;
    private String manufacturer;
    private String model;
    private String appType;
    private String extendModel;
    private String country;
    private String platform;
    private String width;
    private String height;
    private String mcc;
    private String mnc;
    private String lac;
    private String ci;
    private String rxlev;
    private String totalKb;
    private String freeKb;
    private String clientLanguage;
    private String deviceType;

    public static DeviceLoginRequest newInstance() {
        DeviceLoginRequest ret = new DeviceLoginRequest();
        ret.imsi = "68510155565211";
        ret.imei = "6258412454025411";
        ret.manufacturer = "default";
        ret.model = "default";
        ret.appType = "5";
        ret.extendModel = "default";
        ret.country = "default";
        ret.platform = "1";
        ret.width = "240";
        ret.height = "320";
        ret.mcc = "460";
        ret.mnc = "00x";
        ret.lac = "100";
        ret.ci = "5.21";
        ret.rxlev = "-72";
        ret.totalKb = "1424122";
        ret.freeKb = "1424122";
        ret.clientLanguage = "en_US";
        ret.deviceType = "2";
        return ret;
    }

    public String getSessionId() { return imei + "_" + imsi; }

    public String getImsi() { return imsi; }
    public void setImsi(String imsi) { this.imsi = imsi; }
    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getAppType() { return appType; }
    public void setAppType(String appType) { this.appType = appType; }
    public String getExtendModel() { return extendModel; }
    public void setExtendModel(String extendModel) { this.extendModel = extendModel; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getWidth() { return width; }
    public void setWidth(String width) { this.width = width; }
    public String getHeight() { return height; }
    public void setHeight(String height) { this.height = height; }
    public String getMcc() { return mcc; }
    public void setMcc(String mcc) { this.mcc = mcc; }
    public String getMnc() { return mnc; }
    public void setMnc(String mnc) { this.mnc = mnc; }
    public String getLac() { return lac; }
    public void setLac(String lac) { this.lac = lac; }
    public String getCi() { return ci; }
    public void setCi(String ci) { this.ci = ci; }
    public String getRxlev() { return rxlev; }
    public void setRxlev(String rxlev) { this.rxlev = rxlev; }
    public String getTotalKb() { return totalKb; }
    public void setTotalKb(String totalKb) { this.totalKb = totalKb; }
    public String getFreeKb() { return freeKb; }
    public void setFreeKb(String freeKb) { this.freeKb = freeKb; }
    public String getClientLanguage() { return clientLanguage; }
    public void setClientLanguage(String clientLanguage) { this.clientLanguage = clientLanguage; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
}
