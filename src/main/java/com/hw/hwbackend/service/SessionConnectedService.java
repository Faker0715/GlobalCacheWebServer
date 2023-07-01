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
    private final MenuMapper menuMapper;

    @Autowired
    public SessionConnectedService(RegMapper regMapper, MenuMapper menuMapper) {
        this.regMapper = regMapper;
        this.menuMapper = menuMapper;
    }

    private static Logger log = LoggerFactory.getLogger(SessionConnectedService.class);

    public void func() {
        System.out.println("SessionConnectedService start");
        List<GlobalCacheUser> globalCacheUsers = regMapper.getuser();
        for (Map.Entry<ArrayList<String>, Boolean> entry : UserHolder.getInstance().getClusterCephMap().entrySet()) {
            ArrayList<String> host2 = entry.getKey();
            boolean flag = true;
            for (int i = 0; i < host2.size(); i++) {
                for (int j = 0; j < globalCacheUsers.size(); j++) {
                    try {
                        flag = GlobalCacheSDK.isSessionConnected(host2.get(i), globalCacheUsers.get(j).getUsername());
                    } catch (GlobalCacheSDKException e) {
                        System.out.println("接口调用失败");
                        e.printStackTrace();
                    }
                }
            }
            UserHolder.getInstance().getClusterCephMap().put(entry.getKey(), flag);
            UserHolder.getInstance().getClusterMap().put(entry.getKey().get(0), flag);
        }

        System.out.println("first check: " + UserHolder.getInstance().getClusterCephMap().toString());
        System.out.println("first check: " + UserHolder.getInstance().getClusterMap().toString());

        for (Map.Entry<ArrayList<String>, Boolean> entry : UserHolder.getInstance().getClusterCephMap().entrySet()) {
            if(entry.getValue() == false){
                boolean flag = true;
                ArrayList<String> host2 = entry.getKey();
                for (int i = 0; i < host2.size(); i++) {
                    for (int j = 0; j < globalCacheUsers.size(); j++) {
                        String password = globalCacheUsers.get(j).getPassword();
                        try {
                            GlobalCacheSDK.createSession(host2.get(i), globalCacheUsers.get(j).getUsername(), password, 22);
                        } catch (GlobalCacheSDKException e) {
                            e.printStackTrace();
                            System.out.println("reconnected失败 " + host2.get(i) + " " + globalCacheUsers.get(j).getUsername());
                            System.out.println("createSession失败 " + host2.get(i) + " " + globalCacheUsers.get(j).getUsername());
                            flag = false;
                        }
                    }
                }
                UserHolder.getInstance().getClusterCephMap().put(entry.getKey(), flag);
                UserHolder.getInstance().getClusterMap().put(entry.getKey().get(0), flag);
            }
        }
        System.out.println("second check: " + UserHolder.getInstance().getClusterCephMap().toString());
        System.out.println("second check: " + UserHolder.getInstance().getClusterMap().toString());
    }


}
