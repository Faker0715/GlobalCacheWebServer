package com.hw.hwbackend.service;


import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.exception.GlobalCacheSDKException;
import com.hw.hwbackend.entity.GlobalCacheUser;
import com.hw.hwbackend.mapper.MenuMapper;
import com.hw.hwbackend.mapper.RegMapper;
import com.hw.hwbackend.util.UserHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SessionConnectedService {

    private final RegMapper regMapper;

    @Autowired
    public SessionConnectedService(RegMapper regMapper) {
        this.regMapper = regMapper;
    }

    private static Logger log = LoggerFactory.getLogger(SessionConnectedService.class);

    public void func() {
        System.out.println("SessionConnectedService start");
        List<GlobalCacheUser> globalCacheUsers = regMapper.getuser();
        for (Map.Entry<String, Boolean> entry : UserHolder.getInstance().getClusterMap().entrySet()) {
            boolean flag = true;
            for (int j = 0; j < globalCacheUsers.size(); j++) {
                try {
                    flag = GlobalCacheSDK.isSessionConnected(entry.getKey(), globalCacheUsers.get(j).getUsername());
                } catch (GlobalCacheSDKException e) {
                    System.out.println("接口调用失败");
                    e.printStackTrace();
                }
            }
            UserHolder.getInstance().setCeph1(entry.getKey());
            UserHolder.getInstance().getClusterMap().put(entry.getKey(), flag);
        }

        System.out.println("first check: " + UserHolder.getInstance().getClusterMap().toString());

        for (Map.Entry<String, Boolean> entry : UserHolder.getInstance().getClusterMap().entrySet()) {
            if(entry.getValue() == false){
                boolean flag = true;
                for (int j = 0; j < globalCacheUsers.size(); j++) {
                    String password = globalCacheUsers.get(j).getPassword();
                    try {
                        GlobalCacheSDK.createSession(entry.getKey(), globalCacheUsers.get(j).getUsername(), password, 22);
                    } catch (GlobalCacheSDKException e) {
                        e.printStackTrace();
                        System.out.println("reconnected失败 " + entry.getKey() + " " + globalCacheUsers.get(j).getUsername());
                        System.out.println("createSession失败 " + entry.getKey() + " " + globalCacheUsers.get(j).getUsername());
                        flag = false;
                    }
                }
                UserHolder.getInstance().getClusterMap().put(entry.getKey(), flag);
            }
        }
        System.out.println("second check: " + UserHolder.getInstance().getClusterMap().toString());
    }


}
