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
    private final MenuMapper menuMapper;

    private static Logger log = LoggerFactory.getLogger(SessionService.class);
    @Autowired
    public SessionService(RegMapper regMapper, MenuMapper menuMapper) {
        this.regMapper = regMapper;
        this.menuMapper = menuMapper;
        initSession();
    }

    public void initSession() {
        if (regMapper.getfinished() == 1) {
            // 得到所有的映射
            List<Ceph> cephs = menuMapper.selectCephs();
            List<GlobalCacheUser> globalCacheUsers = regMapper.getuser();
            String ceph1 = regMapper.getCeph1Ip();
            UserHolder.getInstance().setCeph1(ceph1);
            UserHolder.getInstance().setSuccess(true);
            log.info("sessionsevice: setceph1ip: " + ceph1);
            ArrayList<String> hosts = new ArrayList<>();
            for (Ceph ceph : cephs) {
                hosts.add(ceph.getIp());
                log.info("sessionsevice: add ip to cephs " + ceph.getIp());
            }

            for (int i = 0; i < hosts.size(); i++) {
                for (int j = 0; j < globalCacheUsers.size(); j++) {
                    String password1 =  globalCacheUsers.get(j).getPassword();
                    try {
                        GlobalCacheSDK.createSession(hosts.get(i), globalCacheUsers.get(j).getUsername(), password1, 22);
                    } catch (GlobalCacheSDKException e) {
                        System.out.println("createSession失败 " + hosts.get(i) + " " + globalCacheUsers.get(j).getUsername());
                        e.printStackTrace();
                    }
                }

            }

            // 程序已启动先更新node表
            Iprelation ip = new Iprelation();
            Map<Integer, String> idmap = new HashMap<>();
            HashMap<Integer, ArrayList<Integer>> disks = new HashMap<>();
            ArrayList<Integer> nodes = new ArrayList<>();
            ArrayList<String> ips = new ArrayList<>();
            NodeStatusInfo nodeStatusInfo = new NodeStatusInfo();
            int tryTimes = 5;
            int oldTimeout = 0;
            try {
                // 记录当前接口默认超时等待时间
                oldTimeout = GlobalCacheSDK.getCommandConf(RegisterExecutor.QUERY_NODE_STATUS_INFO).getTimeout();
            } catch (GlobalCacheSDKException e) {
                System.out.println("获取执行时间失败");
                e.printStackTrace();
            }
            // 时长增长因子
            double increaseFactor = 1.5f;
            // 当前接口超时等待时间
            int curTimeout = oldTimeout;
            boolean flag = false;
            for (int i = 0; i < tryTimes; ++i) {
                if (flag)
                    break;
                try {
                    for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryNodeStatusInfo(ceph1).entrySet()) {
                        if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                            nodeStatusInfo = (NodeStatusInfo) (entry.getValue().getData());
                            for (NodeStatusInfo.Node node : nodeStatusInfo.getNodeList()) {
                                idmap.put(node.getNodeId(), node.getClusterIp());
                                nodes.add(node.getNodeId());
                                ips.add(node.getClusterIp());
                                ArrayList<Integer> diskarray = new ArrayList<>();
                                for (NodeStatusInfo.Disk disk : node.getDisks()) {
                                    diskarray.add(disk.getDiskId());
                                }
                                disks.put(node.getNodeId(), diskarray);
                            }
                            flag = true;
                        } else if (entry.getValue().getStatusCode() == StatusCode.EXEC_COMMAND_FAILED) {
                            // 处理线程执行中断的情况 -> 休眠，等待下一次尝试
                            sleep(oldTimeout);
                        } else if (entry.getValue().getStatusCode() == StatusCode.EXEC_COMMAND_TIMEOUT) {
                            // 处理请求超时的情况 -> 增长至 increaseFactor * curTimeout
                            curTimeout = (int) Math.ceil(curTimeout * increaseFactor);
                            GlobalCacheSDK.setCommandTimeout(RegisterExecutor.QUERY_NODE_STATUS_INFO, curTimeout);
                        }

                    }
                } catch (GlobalCacheSDKException | InterruptedException e) {
                    System.out.println("接口调用失败");
                    e.printStackTrace();
                }
            }
            try {
                GlobalCacheSDK.setCommandTimeout(RegisterExecutor.QUERY_NODE_STATUS_INFO, oldTimeout);
            } catch (GlobalCacheSDKException e) {
                System.out.println("设置执行时间失败");
                e.printStackTrace();
            }

            ip.setId(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());

            for (int i = 0; i < ips.size(); i++) {

                for (int j = 0; j < globalCacheUsers.size(); j++) {
                    String password1 =  globalCacheUsers.get(j).getPassword();
                    try {
                        GlobalCacheSDK.createSession(ips.get(i), globalCacheUsers.get(j).getUsername(), password1, 22);
                    } catch (GlobalCacheSDKException e) {
                        System.out.println("createSession失败 " + ips.get(i) + " " + globalCacheUsers.get(j).getUsername());
                        e.printStackTrace();
                    }
                }

            }


            Map<String,StaticNetInfo> staticNetInfomap = new HashMap<>();
            for (int i = 0; i < ips.size(); i++) {
                try {
                    for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryStaticNetInfo(hosts.get(i)).entrySet()) {
                        log.info("networksave-querystaticnetinfo: " + entry.getValue().getStatusCode());
                        if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                            log.info("networksave-querystaticnetinfo-data: " + (StaticNetInfo) (entry.getValue().getData()));
                            staticNetInfomap.put(entry.getKey(),(StaticNetInfo) entry.getValue().getData());
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
            UserHolder.getInstance().setIprelation(ip);
            UserHolder.getInstance().setSuccess(true);
        }


    }
}
