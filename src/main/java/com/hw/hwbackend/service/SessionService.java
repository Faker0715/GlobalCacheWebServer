package com.hw.hwbackend.service;

import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.StatusCode;
import com.hw.globalcachesdk.entity.NodeStatusInfo;
import com.hw.globalcachesdk.entity.StaticNetInfo;
import com.hw.globalcachesdk.exception.GlobalCacheSDKException;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import com.hw.globalcachesdk.executor.RegisterExecutor;
import com.hw.hwbackend.entity.Ceph;
import com.hw.hwbackend.entity.GlobalCacheUser;
import com.hw.hwbackend.entity.Iprelation;
import com.hw.hwbackend.entity.User;
import com.hw.hwbackend.mapper.MenuMapper;
import com.hw.hwbackend.mapper.RegMapper;
import com.hw.hwbackend.util.UserHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static java.lang.Thread.sleep;

//@MapperScan("com.hw.hwbackend.mapper")
@Service
public class SessionService {
    private final RegMapper regMapper;
    private static Logger log = LoggerFactory.getLogger(SessionService.class);

    @Autowired
    public SessionService(RegMapper regMapper) {
        this.regMapper = regMapper;
        initSession();
    }

    public void initSession() {
        if (regMapper.getfinished() == 1) {
            // 得到所有的映射
            List<GlobalCacheUser> globalCacheUsers = regMapper.getuser();
            String ceph1 = regMapper.getCeph1Ip();
            UserHolder.getInstance().setCeph1(ceph1);
            HashMap<String, Boolean> clusterMap = new HashMap<>();

            // 程序已启动先更新node表
            Iprelation ip = new Iprelation();
            Map<Integer, String> idmap = new HashMap<>();
            HashMap<Integer, ArrayList<Integer>> disks = new HashMap<>();
            ArrayList<Integer> nodes = new ArrayList<>();
            ArrayList<String> ips = new ArrayList<>();
            try {
                NodeStatusInfo nodeStatusInfo = (NodeStatusInfo) GlobalCacheSDK.queryNodeStatusInfoLocal();
                System.out.println(nodeStatusInfo.toString());
                for (NodeStatusInfo.Node node : nodeStatusInfo.getNodeList()) {
                    idmap.put(node.getNodeId(), node.getClusterIp());
                    clusterMap.put(node.getClusterIp(),false);
                    nodes.add(node.getNodeId());
                    ips.add(node.getClusterIp());
                    ArrayList<Integer> diskarray = new ArrayList<>();
                    for (NodeStatusInfo.Disk disk : node.getDisks()) {
                        diskarray.add(disk.getDiskId());
                    }
                    disks.put(node.getNodeId(), diskarray);
                }
            } catch (GlobalCacheSDKException e) {
                System.out.println("接口调用失败");
                e.printStackTrace();
            }
            ip.setId(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
            for (int i = 0; i < ips.size(); i++) {
                Boolean flag = true;
                for (int j = 0; j < globalCacheUsers.size(); j++) {
                    String password = globalCacheUsers.get(j).getPassword();
                    try {
                        GlobalCacheSDK.createSession(ips.get(i), globalCacheUsers.get(j).getUsername(), password, 22);
                    } catch (GlobalCacheSDKException e) {
                        flag = false;
                        System.out.println("createSession失败 " + ips.get(i) + " " + globalCacheUsers.get(j).getUsername());
                        e.printStackTrace();
                    }
                }
                if(flag){
                    clusterMap.put(ips.get(i),true);
                }
            }

            Map<String, StaticNetInfo> staticNetInfomap = new HashMap<>();
            for (int i = 0; i < ips.size(); i++) {
                try {
                    for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryStaticNetInfo(ips.get(i)).entrySet()) {
                        log.info("networksave-querystaticnetinfo: " + entry.getValue().getStatusCode());
                        if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                            log.info("networksave-querystaticnetinfo-data: " + (StaticNetInfo) (entry.getValue().getData()));
                            staticNetInfomap.put(entry.getKey(), (StaticNetInfo) entry.getValue().getData());
                        }
                    }
                } catch (GlobalCacheSDKException e) {
                    System.out.println("接口调用失败");
                    e.printStackTrace();
                }
            }

            ip.setNodes(nodes);
            ip.setIdMap(idmap);
            ip.setDisks(disks);
            ip.setIps(ips);
            ip.setStaticNetInfomap(staticNetInfomap);
            log.info("sessionservice-iprelation nodes: " + nodes.toString());
            log.info("sessionservice-iprelation idmap: " + idmap.toString());
            log.info("sessionservice-iprelation ips: " + ips.toString());
            UserHolder.getInstance().setClusterMap(clusterMap);
            UserHolder.getInstance().setIprelation(ip);
            UserHolder.getInstance().setSuccess(true);
        }


    }
}
