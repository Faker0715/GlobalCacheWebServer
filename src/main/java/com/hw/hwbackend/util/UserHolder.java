package com.hw.hwbackend.util;

import com.hw.hwbackend.entity.AutoList;
import com.hw.hwbackend.entity.Iprelation;
import lombok.Data;
;import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
// 全局单例类
@Data
public class UserHolder {
    private static UserHolder holder;
    // token -> 自动化部署类
    private ConcurrentHashMap<String, AutoList> autoMap = new ConcurrentHashMap<>();
    private boolean isSuccess = false;
    private BlockingQueue<String> autopipe = new LinkedBlockingQueue<String>();
    private String ceph1 = "";
    private boolean isRunning = false;
    private boolean isReady = false;

    private HashMap<Integer,String> stateMap = new HashMap<>();

    public enum STATE{
        STATE_CONF, // 1
        STATE_COMPILE_SERVER, // 2
        STATE_DISTRIBUTE, // 3
        STATE_COMPILE_CLIENT, // 4
        STATE_CEPH, // 5
        STATE_GCDEPLOY, // 6
        STATE_GCINIT // 7
    }
    private Integer stateNum = 1;

    private UserHolder() {
        urlarray.add("/getCpuCalender");
        urlarray.add("/getCpuData");
        urlarray.add("/getDiskData");
        urlarray.add("/getNetData");
        urlarray.add("/getMemoryData");
        urlarray.add("/getMemoryData");

        stateMap.put(1,"生成集群部署配置文件");
        stateMap.put(2,"服务端依赖包编译");
        stateMap.put(3,"分发依赖包");
        stateMap.put(4,"客户端依赖包编译");
        stateMap.put(5,"ceph部署");
        stateMap.put(6,"globalcache部署");
        stateMap.put(7,"globalcache部署完成");


    }

    public void addAuto(String token, AutoList auto) {
        autoMap.put(token, auto);
    }

    public void deleteAuto(String token) {
        autoMap.remove(token);
    }

    public static UserHolder getInstance() {
        if (holder == null) {
            holder = new UserHolder();
        }
        return holder;
    }

    private Iprelation iprelation;
    private ArrayList<String> urlarray = new ArrayList<>();

    public Iprelation getIprelation() {
        return iprelation;
    }

    public void setIprelation(Iprelation iprelation) {
        this.iprelation = iprelation;
    }
}
