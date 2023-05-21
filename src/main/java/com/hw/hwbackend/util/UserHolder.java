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

    private HashMap<STATE,String> stateMap = new HashMap<>();

    public enum STATE{
        STATE_NULL,
        STATE_CONF,
        STATE_COMPILE_SERVER,
        STATE_DISTRIBUTE,
        STATE_COMPILE_CLIENT,
        STATE_CEPH,
        STATE_GCDEPLOY,
        STATE_GCINIT
    }
    private STATE state = STATE.STATE_NULL;
    private Integer stateNum = 0;

    private UserHolder() {
        urlarray.add("/getCpuCalender");
        urlarray.add("/getCpuData");
        urlarray.add("/getDiskData");
        urlarray.add("/getNetData");
        urlarray.add("/getMemoryData");
        urlarray.add("/getMemoryData");

        stateMap.put(STATE.STATE_NULL,"正在开始");
        stateMap.put(STATE.STATE_CONF,"生成集群部署配置文件");
        stateMap.put(STATE.STATE_COMPILE_SERVER,"服务端依赖包编译");
        stateMap.put(STATE.STATE_DISTRIBUTE,"分发依赖包");
        stateMap.put(STATE.STATE_COMPILE_CLIENT,"客户端依赖包编译");
        stateMap.put(STATE.STATE_CEPH,"ceph部署");
        stateMap.put(STATE.STATE_GCDEPLOY,"globalcache部署");

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
