package com.huawei.browsergateway.util.encode;

import lombok.Data;

@Data
public class TlvField {
    private int type;
    private int len;
    private byte[] data;

    public TlvField(){}

    public TlvField(int type, int len, byte[] data) {
        this.type = type;
        this.len = len;
        this.data = data;
    }

    public void setType(int type){this.type = type;}
    public void setLen(int len){this.len = len;}
    public void setData(byte[] data){this.data = data;}

    public int getInt(){return this.type;}
}
