package com.hw.hwbackend.util;

import com.hw.hwbackend.entity.AutoList;
import com.hw.hwbackend.entity.Iprelation;
import com.hw.hwbackend.entity.User;
import lombok.Data;
;import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
// 全局单例类
@Data
public class UserHolder {
    private static UserHolder holder;
    // token -> 自动化部署类
    private ConcurrentHashMap<String, AutoList> autoMap = new ConcurrentHashMap<>();
    private boolean isdeployfinished = false;
    private boolean isSuccess = false;
    private BlockingQueue<String> autopipe = new LinkedBlockingQueue<String>();
    private String ceph1 = "";
    private UserHolder() {
        urlarray.add("/getCpuCalender");
        urlarray.add("/getCpuData");
        urlarray.add("/getDiskData");
        urlarray.add("/getNetData");
        urlarray.add("/getMemoryData");
        urlarray.add("/getMemoryData");
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
