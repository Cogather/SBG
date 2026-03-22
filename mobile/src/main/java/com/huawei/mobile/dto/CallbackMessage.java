package com.huawei.mobile.dto;

public class CallbackMessage {
    private String type;
    private Integer elm;
    private Integer info;
    private String content;
    private Integer wt;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getElm() { return elm; }
    public void setElm(Integer elm) { this.elm = elm; }
    public Integer getInfo() { return info; }
    public void setInfo(Integer info) { this.info = info; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getWt() { return wt; }
    public void setWt(Integer wt) { this.wt = wt; }
}
