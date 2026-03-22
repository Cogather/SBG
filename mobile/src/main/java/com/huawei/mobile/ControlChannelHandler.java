package com.huawei.mobile;

import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.huawei.mobile.common.ID;
import com.huawei.mobile.common.Type;
import com.huawei.mobile.dto.CallbackMessage;
import com.huawei.mobile.encode.TlbData;
import com.huawei.mobile.encode.TlvData;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ControlChannelHandler extends ChannelInboundHandlerAdapter {
    private static final Log log = LogFactory.get();
    private BrowserContext browserContext;

    public ControlChannelHandler(BrowserContext browserContext) {
        this.browserContext = browserContext;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        TlbData data = (TlbData) msg;
        try {
            Object typeObj = data.get(ID.TYPE);
            if (typeObj == null) return;
            int type;
            if (typeObj instanceof Integer) {
                type = (Integer) typeObj;
            } else {
                type = Integer.parseInt(typeObj.toString());
            }
            if (type == Type.ACK) {
                handleAck(data);
            } else if (type == Type.RETURN_MEDIA) {
                handleMediaAddress(data);
            } else if (type == Type.RETURN_CONTROL) {
                handleControlResponse(data);
            } else {
                log.debug("control channel received type {}", type);
            }
        } finally {
            data.clear();
        }
    }

    private void handleAck(TlbData data) {
        log.debug("control channel ACK received");
    }

    private void handleMediaAddress(TlbData data) {
        Object addrObj = data.get(ID.TCP_ADDR);
        if (addrObj == null) return;
        String tcpAddr = addrObj.toString();
        log.info("RETURN_MEDIA: new media addr {}", tcpAddr);
        browserContext.getMediaAddr().set(tcpAddr);
        String[] parts = tcpAddr.split(":");
        browserContext.doConnectMediaChannel(parts[0], Integer.parseInt(parts[1]));
    }

    private void handleControlResponse(TlbData data) {
        CallbackMessage cb = new CallbackMessage();
        Object elmObj = data.get(ID.CTRL_RSP_ELM);
        Object infoObj = data.get(ID.CTRL_RSP_INFO);
        Object contentObj = data.get(ID.CONTENT);
        Object wtObj = data.get(ID.WRITE_TYPE);
        cb.setType("callback");
        if (elmObj != null) cb.setElm(elmObj instanceof Integer ? (Integer) elmObj : Integer.parseInt(elmObj.toString()));
        if (infoObj != null) cb.setInfo(infoObj instanceof Integer ? (Integer) infoObj : Integer.parseInt(infoObj.toString()));
        if (contentObj != null) cb.setContent(contentObj.toString());
        if (wtObj != null) cb.setWt(wtObj instanceof Integer ? (Integer) wtObj : Integer.parseInt(wtObj.toString()));
        browserContext.getSession().sendText(JSONUtil.toJsonStr(cb));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("exceptionCaught", cause);
        ctx.close();
    }
}
