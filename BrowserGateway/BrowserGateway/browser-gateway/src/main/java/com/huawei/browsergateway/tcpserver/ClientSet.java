package com.huawei.browsergateway.tcpserver;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientSet {
    protected final Map<String, Client> map = new ConcurrentHashMap<>();


    public void set(String key, Client cli) {
        Client oldCli = map.get(key);
        if (oldCli != null) {
            oldCli.close();
        }
        map.put(key, cli);
    }

    public void del(String key) {
        Client remove = map.remove(key);
        if (remove != null) {
            remove.close();
        }
    }

    public void del(Client cli) {
        String key = cli.getStr(Client.VAL_SESSION_ID);
        if (key != null) {
            del(key);
        }
    }

    public Client get(String key) {
        return map.get(key);
    }

    public Set<String> allClient() {
        return map.keySet();
    }
}