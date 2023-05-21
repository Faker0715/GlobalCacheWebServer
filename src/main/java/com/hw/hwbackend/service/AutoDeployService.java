package com.hw.hwbackend.service;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;
import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.StatusCode;
import com.hw.globalcachesdk.entity.*;
import com.hw.globalcachesdk.exception.AsyncThreadException;
import com.hw.globalcachesdk.exception.GlobalCacheSDKException;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import com.hw.hwbackend.entity.AutoList;
import com.hw.hwbackend.entity.Ceph;
import com.hw.hwbackend.mapper.MenuMapper;
import com.hw.hwbackend.mapper.RegMapper;
import com.hw.hwbackend.util.ResponseResult;
import com.hw.hwbackend.util.UserHolder;
import io.lettuce.core.output.ScanOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.hw.globalcachesdk.ExecuteNode.CEPH1_ONLY;
import static com.hw.globalcachesdk.ExecuteNode.ALL_CLIENT_NODES;
import static com.hw.hwbackend.util.UserHolder.STATE.*;

//自动化部署的业务类
@Service
public class AutoDeployService {

    @Autowired
    private RegMapper regMapper;
    @Autowired
    private MenuMapper menuMapper;


    @Autowired
    private SessionService sessionService;
    private static Logger log = LoggerFactory.getLogger(AutoDeployService.class);

    //验证密码
    public ResponseResult getCheckRootPassword(String password, String token) {
        Map<String, Object> returnmap = new HashMap<>();
        UserHolder userHolder = UserHolder.getInstance();
        if (BeanUtil.isEmpty(userHolder.getAutoMap().get(token))) {
            userHolder.addAuto(token, new AutoList());
        }
        userHolder.getAutoMap().get(token).setPassword(password);
        if (BeanUtil.isEmpty(userHolder.getAutoMap().get(token).getAutoEntityArrayList())) {
            userHolder.getAutoMap().get(token).setAutoEntityArrayList(new ArrayList<>());
        }
        returnmap.put("isRight", true);
        return new ResponseResult<Map<String, Object>>(returnmap);
    }

    //添加节点
    public ResponseResult getAddIP(String ipAddress, String token) {
        boolean isValid = false;
        UserHolder userHolder = UserHolder.getInstance();
        Map<String, Object> map = new HashMap<>();
        boolean isConnected = true;
        boolean isCpu = true;
        boolean isMemory = true;
        boolean isSave = false;
        //判断连接是否存在
        try {
            isSave = GlobalCacheSDK.isSessionExist(ipAddress, "root");
            if (isSave) {
                isValid = true;
            }
        } catch (GlobalCacheSDKException e) {
            e.printStackTrace();
        }
        //连接不存在就创建连接
        if (isSave == false) {
            try {
                GlobalCacheSDK.createSession(ipAddress, "root", userHolder.getAutoMap().get(token).getPassword(), 22);
                isValid = true;
            } catch (GlobalCacheSDKException e) {
                e.printStackTrace();
            }
        } else {
            isValid = true;
        }
        if (isValid == false || ipAddress.equals("0.0.0.0") || ipAddress.equals("127.0.0.1")) {
            map.put("isValid", false);
            map.put("reason", "输入ip错误");
            map.put("isConnected", false);
            map.put("isCpu", false);
            map.put("isMemory", false);
            return new ResponseResult<Map<String, Object>>(map);
        }

        Map<String, ErrorCodeEntity> entityMap = new HashMap<>(1);
        ArrayList<String> hosts = new ArrayList<>();
        hosts.add(ipAddress);
        //获取节点状态
        try {
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.checkHardware().entrySet()) {
                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                    entityMap.put(entry.getKey(), (ErrorCodeEntity) entry.getValue().getData());
                } else {
                    System.out.println("接口调用失败");
//                    System.out.println(entry.getValue().getStatusCode());
                }
            }
        } catch (GlobalCacheSDKException e) {
            System.out.println("接口调用失败");
            e.printStackTrace();
        }
        //根据获取的节点状态 设置返回信息
        for (Map.Entry<String, ErrorCodeEntity> entry : entityMap.entrySet()) {
            if (entry.getValue().getErrorCode() != 0) {
                isConnected = false;
            }
            if (entry.getValue().getErrorCode() == 1) {
                isCpu = false;
            }
            if (entry.getValue().getErrorCode() == 2) {
                isMemory = false;
            }
        }
        //创建返回信息
        map.put("isValid", isValid);
        if (!isConnected) {
            map.put("reason", "未连接成功");
        } else if (!isCpu) {
            map.put("reason", "cpu不合法");
        } else if (!isMemory) {
            map.put("reason", "内存不合法");
        }


        String rolename = "";
        if (isConnected) {
            try {
                for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryHostNameInfo(ipAddress).entrySet()) {
                    if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                        HostNameInfo hostNameInfo = (HostNameInfo) (entry.getValue().getData());
                        rolename = hostNameInfo.getHostname();
                        System.out.println("rolename " + rolename);
                    } else {
                        System.out.println("接口调用失败");
                    }
                }
            } catch (GlobalCacheSDKException e) {
                System.out.println("接口调用失败");
                e.printStackTrace();
            }
        } else {
            rolename = "";
        }
        boolean ceph = false;
        boolean ceph1 = false;
        boolean client = false;
        map.put("roleName", rolename);
        if (rolename.equals("ceph1")) {
            ceph1 = true;
            map.put("ceph", false);
            map.put("ceph1", true);
            map.put("client", false);
        } else if (rolename.contains("ceph")) {
            ceph = true;
            map.put("ceph", true);
            map.put("ceph1", false);
            map.put("client", false);
        } else {
            client = true;
            map.put("ceph", false);
            map.put("ceph1", false);
            map.put("client", false);
        }

        map.put("isConnected", isConnected);
        map.put("isCpu", isCpu);
        map.put("isMemory", isMemory);
        if (isConnected && isCpu && isMemory) {
            int flag = 0;
            for (int i = 0; i < userHolder.getAutoMap().get(token).getAutoEntityArrayList().size(); ++i) {
                if (userHolder.getAutoMap().get(token).getAutoEntityArrayList().get(i).getName().equals(ipAddress)) {
                    flag = 1;
                    break;
                }
            }
            if (flag == 0) {
                AutoList.AutoEntity autoEntity = new AutoList.AutoEntity();
                autoEntity.setName(ipAddress);
                autoEntity.setisConnected(isConnected);
                autoEntity.setisCpu(isCpu);
                autoEntity.setisMemory(isMemory);
                autoEntity.setLocalIPv4(ipAddress);
                autoEntity.setClusterIPv4(ipAddress);
                autoEntity.setCeph(ceph);
                autoEntity.setCeph1(ceph1);
                autoEntity.setClient(client);
                autoEntity.setRoleName(rolename);

                DiskInfo diskInfo = new DiskInfo();
                try {
                    for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryDiskInfo(hosts).entrySet()) {
                        if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                            diskInfo = (DiskInfo) entry.getValue().getData();
                            ArrayList<DiskInfo.Disk> disksList = diskInfo.getDisksList();
                            ArrayList<AutoList.AutoEntity.DataDisk> dataDisks = new ArrayList<>();
                            ArrayList<AutoList.AutoEntity.CacheDisk> cacheDisks = new ArrayList<>();
                            int dataid = 0;
                            int cacheid = 0;
                            for (DiskInfo.Disk disk : disksList) {
                                if (disk.getType() == DiskInfo.DiskType.ROTA) {
                                    AutoList.AutoEntity.DataDisk dataDisk = new AutoList.AutoEntity.DataDisk();
                                    dataDisk.setId(dataid++);
                                    dataDisk.setType("SD");
                                    dataDisk.setName(disk.getName());
                                    dataDisks.add(dataDisk);
                                } else {
                                    AutoList.AutoEntity.CacheDisk cacheDisk = new AutoList.AutoEntity.CacheDisk();
                                    cacheDisk.setId(cacheid++);
                                    cacheDisk.setType("nvme");
                                    cacheDisk.setName(disk.getName());
                                    cacheDisks.add(cacheDisk);
                                }
                            }
                            //保存
                            autoEntity.setCacheDisk(cacheDisks);
                            autoEntity.setDataDisk(dataDisks);
                            System.out.println("cachedisks: " + cacheDisks.size() + " datadisks: " + dataDisks.size());
                        } else {
                            System.out.println(entry.getValue().getStatusCode());
                        }
                    }
                } catch (GlobalCacheSDKException e) {
                    System.out.println("接口调用失败");
                    e.printStackTrace();
                }
                List<AutoList.AutoEntity> list = new ArrayList<>();
                userHolder.getAutoMap().get(token).getAutoEntityArrayList().add(autoEntity);
            }
        }
        return new ResponseResult<Map<String, Object>>(map);
    }

    //删除节点
    public ResponseResult getDeleteIP(String ipAddress, String token) {
        boolean isValid = false;
        //获取当前已经保存的节点
        UserHolder userHolder = UserHolder.getInstance();
        List<AutoList.AutoEntity> list = userHolder.getAutoMap().get(token).getAutoEntityArrayList();
        //从列表中删除
        try {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getName().equals(ipAddress)) {
                    list.remove(list.get(i));
                    break;
                }
            }
            isValid = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        userHolder.getAutoMap().get(token).setAutoEntityArrayList(list);

        Map<String, Object> map = new HashMap<>();
        map.put("isValid", isValid);
        return new ResponseResult<Map<String, Object>>(map);
    }

    //获取已经保存的节点信息
    public ResponseResult getIpList(String token) {
        //获取信息
        UserHolder userHolder = UserHolder.getInstance();
        List<AutoList.AutoEntity> ipList = userHolder.getAutoMap().get(token).getAutoEntityArrayList();
        Map map = new HashMap<>();
        map.put("ipList", ipList);
//        System.out.println(ipList);
        return new ResponseResult<Map<String, Object>>(map);
    }

    //检查硬件状态
    public ResponseResult getHardwareDetect(String ipAddress, String token) {
        Map<String, Object> map = new HashMap<>();
        ArrayList<String> hosts = new ArrayList<>();
        hosts.add(ipAddress);
        Map<String, ErrorCodeEntity> entityMap = new HashMap<>(hosts.size());
        //检查硬件状态
        try {
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.checkHardware().entrySet()) {
                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                    entityMap.put(entry.getKey(), (ErrorCodeEntity) entry.getValue().getData());
                } else {
                    System.out.println("接口调用失败");
//                    System.out.println(entry.getValue().getStatusCode());
                }
            }
        } catch (GlobalCacheSDKException e) {
            System.out.println("接口调用失败");
            e.printStackTrace();
        }

        //根据硬件状态 设置返回信息
        boolean isConnected = true;
        boolean isCpu = true;
        boolean isMemory = true;
        for (Map.Entry<String, ErrorCodeEntity> entry : entityMap.entrySet()) {
//            System.out.println(entry.getKey() + entry.getValue().getErrorCode() + entry.getValue().getMessage());
            if (entry.getValue().getErrorCode() != 0) {
                isConnected = false;
            }
            if (entry.getValue().getErrorCode() == 1) {
                isCpu = false;
            }
            if (entry.getValue().getErrorCode() == 2) {
                isMemory = false;
            }
        }

        map.put("name", ipAddress);
        map.put("isConnected", isConnected);
        map.put("isCpu", isCpu);
        map.put("isMemory", isMemory);
        return new ResponseResult<Map<String, Object>>(map);

    }

    //设置节点的角色
    public ResponseResult getRoleSet(Map map) {
        //获取当前保存的所有节点信息
        String str = JSONObject.toJSONString(map.get("ipList"));
        List<AutoList.AutoEntity> allEntity = JSONObject.parseArray(str, AutoList.AutoEntity.class);
        UserHolder userHolder = UserHolder.getInstance();
        String token = (String) map.get("token");
        ArrayList<String> hosts = new ArrayList<>();
        //如果节点的角色是Ceph或者Ceph1 为其查询磁盘信息
        for (AutoList.AutoEntity entity : allEntity) {
            if (entity.Ceph() || entity.Ceph1()) {
                hosts.add(entity.getName());
            }
        }

        DiskInfo diskInfo = new DiskInfo();
        for (int i = 0; i < hosts.size(); ++i) {
            ArrayList<String> host = new ArrayList<>();
            host.add(hosts.get(i));
            //获取磁盘信息
            try {
                for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryDiskInfo(host).entrySet()) {
                    if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                        diskInfo = (DiskInfo) entry.getValue().getData();
                        ArrayList<DiskInfo.Disk> disksList = diskInfo.getDisksList();
                        for (int j = 0; j < allEntity.size(); j++) {

                            ArrayList<AutoList.AutoEntity.DataDisk> dataDisks = new ArrayList<>();
                            ArrayList<AutoList.AutoEntity.CacheDisk> cacheDisks = new ArrayList<>();
                            int dataid = 0;
                            int cacheid = 0;
                            if (allEntity.get(j).getName() == hosts.get(i)) {
                                for (DiskInfo.Disk disk : disksList) {
                                    if (disk.getType() == DiskInfo.DiskType.ROTA) {
                                        AutoList.AutoEntity.DataDisk dataDisk = new AutoList.AutoEntity.DataDisk();
                                        dataDisk.setId(dataid++);
                                        dataDisk.setType("SD");
                                        dataDisk.setName(disk.getName());
                                        dataDisks.add(dataDisk);
                                    } else {
                                        AutoList.AutoEntity.CacheDisk cacheDisk = new AutoList.AutoEntity.CacheDisk();
                                        cacheDisk.setId(cacheid++);
                                        cacheDisk.setType("nvme");
                                        cacheDisk.setName(disk.getName());
                                        cacheDisks.add(cacheDisk);
                                    }
                                }
                                //保存
                                allEntity.get(j).setCacheDisk(cacheDisks);
                                allEntity.get(j).setDataDisk(dataDisks);
                                break;
                            }
                        }
                    } else {
                        System.out.println(entry.getValue().getStatusCode());
                        log.info("" + entry.getValue().getStatusCode());
                    }
                }
            } catch (GlobalCacheSDKException e) {
                System.out.println("接口调用失败");
                e.printStackTrace();
            }
        }
        List<AutoList.AutoEntity> list = userHolder.getAutoMap().get(token).getAutoEntityArrayList();
        //将查询到的信息保存
        for (int i = 0; i < list.size(); i++) {
            for (int j = 0; j < allEntity.size(); j++) {
                if (list.get(i).getName().equals(allEntity.get(j).getName())) {
                    allEntity.get(j).setisCpu(list.get(i).getisCpu());
                    allEntity.get(j).setisConnected(list.get(i).getisConnected());
                    allEntity.get(j).setisMemory(list.get(i).getisMemory());

                    allEntity.get(j).setLocalIPv4(list.get(i).getLocalIPv4());
                    allEntity.get(j).setClusterIPv4(list.get(i).getClusterIPv4());

                }
            }

        }
        userHolder.getAutoMap().get(token).setAutoEntityArrayList(allEntity);
        HashMap<String, Object> returnmap = new HashMap<>();
        map.put("isSuccessed", true);
        return new ResponseResult<Map<String, Object>>(returnmap);
    }

    //设置节点的ip
    public ResponseResult getIPSet(Map map) {
        //获取前端设置的信息
        String str = JSONObject.toJSONString(map.get("ipList"));
        List<AutoList.AutoEntity> jsonArray = JSONObject.parseArray(str, AutoList.AutoEntity.class);
        UserHolder userHolder = UserHolder.getInstance();
        String token = (String) map.get("token");
        //获取当前保存的所有节点信息
        List<AutoList.AutoEntity> ipList = userHolder.getAutoMap().get(token).getAutoEntityArrayList();
        //设置ipv4和ipv6地址
        for (int i = 0; i < ipList.size(); i++) {
            for (int j = 0; j < jsonArray.size(); j++) {
                if (ipList.get(i).getName().equals(jsonArray.get(j).getName())) {
                    ipList.get(i).setLocalIPv4(jsonArray.get(j).getLocalIPv4());
                    ipList.get(i).setClusterIPv4(jsonArray.get(j).getClusterIPv4());
                    ipList.get(i).setRemoteIPv4(jsonArray.get(j).getRemoteIPv4());
                }
            }
        }
        //保存
        userHolder.getAutoMap().get(token).setAutoEntityArrayList(ipList);
        HashMap<String, Object> returnmap = new HashMap<>();
//        System.out.println(userHolder.getAutoMap().get(token).getAutoEntityArrayList());
        map.put("isSuccessed", true);
        return new ResponseResult<Map<String, Object>>(returnmap);
    }

    //设置磁盘信息
    public ResponseResult getDiskClassification(Map data) {
        //获取前端设置的信息
        String str = JSONObject.toJSONString(data.get("ipList"));
        List<AutoList.AutoEntity> allEntity = JSONObject.parseArray(str, AutoList.AutoEntity.class);
        UserHolder userHolder = UserHolder.getInstance();
        String token = (String) data.get("token");
        //获取当前保存的所有节点信息
        List<AutoList.AutoEntity> autolist = userHolder.getAutoMap().get(token).getAutoEntityArrayList();

        //设置磁盘信息
        for (AutoList.AutoEntity entity : autolist) {
            for (int i = 0; i < allEntity.size(); i++) {
                if (allEntity.get(i).getName().equals(entity.getName())) {
//                    System.out.println(allEntity.get(i));
//                    System.out.println(allEntity.get(i).getDataList());
                    entity.setDataList(allEntity.get(i).getDataList());
                    entity.setCacheList(allEntity.get(i).getCacheList());
                }
            }
        }
        //保存
        userHolder.getAutoMap().get(token).setAutoEntityArrayList(autolist);
        HashMap<String, Object> returnmap = new HashMap<>();
        returnmap.put("isSuccessed", true);
        return new ResponseResult<Map<String, Object>>(returnmap);
    }

    //设置集群信息
    public ResponseResult getClusterSet(Map data) {
        //获取前端设置的信息
        String str = JSONObject.toJSONString(data);
        JSONObject jsonobject = JSONObject.parseObject(str);
        UserHolder userHolder = UserHolder.getInstance();
        //获取当前保存的所有节点信息
        AutoList autolist = userHolder.getAutoMap().get((String) jsonobject.get("token"));
        //设置集群信息
        autolist.setPnet((String) jsonobject.get("pnet"));
        autolist.setCnet((String) jsonobject.get("cnet"));
        autolist.setPubMask((String) jsonobject.get("pubMask"));
        autolist.setCluMask((String) jsonobject.get("cluMask"));
        autolist.setPtNum(jsonobject.getIntValue("ptNum"));
        autolist.setPgNum(jsonobject.getIntValue("pgNum"));
        //保存
        userHolder.getAutoMap().put((String) jsonobject.get("token"), autolist);
        HashMap returnmap = new HashMap<>();
        returnmap.put("isSuccessed", true);
        return new ResponseResult(returnmap);
    }

    //返回设置的所有信息
    public ResponseResult getAffirmSet(Map data) {

        String str = JSONObject.toJSONString(data);
        JSONObject jsonobject = JSONObject.parseObject(str);
        UserHolder userHolder = UserHolder.getInstance();
        //获取当前保存的所有节点信息
        AutoList autolist = userHolder.getAutoMap().get((String) jsonobject.get("token"));
        HashMap<String, Object> returnmap = new HashMap<>();

        //设置返回信息
        returnmap.put("pnet", autolist.getPnet());
        returnmap.put("cnet", autolist.getCnet());
        returnmap.put("pubMask", autolist.getPubMask());
        returnmap.put("cluMask", autolist.getPubMask());
        returnmap.put("ptNum", autolist.getPtNum());
        returnmap.put("pgNum", autolist.getPgNum());

        List<AutoList.AutoEntity> ceph1 = new ArrayList<>();
        List<AutoList.AutoEntity> ceph = new ArrayList<>();
        List<AutoList.AutoEntity> client = new ArrayList<>();
        //根据角色分类
        List<AutoList.AutoEntity> list = autolist.getAutoEntityArrayList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).Ceph()) {
                ceph.add(list.get(i));
            }
            if (list.get(i).Ceph1()) {
                ceph1.add(list.get(i));
            }
            if (list.get(i).Client()) {
                client.add(list.get(i));
            }
        }
        returnmap.put("ceph1", ceph1);
        returnmap.put("ceph", ceph);
        returnmap.put("client", client);
        return new ResponseResult<Map<String, Object>>(returnmap);
    }

    //返回集群设置信息
    public ResponseResult getClusterInfo(Map data) {
        //获取前端设置的信息
        String str = JSONObject.toJSONString(data);
        JSONObject jsonobject = JSONObject.parseObject(str);
        UserHolder userHolder = UserHolder.getInstance();
        //获取当前保存的所有节点信息
        AutoList autolist = userHolder.getAutoMap().get((String) jsonobject.get("token"));

        HashMap<String, Object> returnmap = new HashMap<>();
        //设置返回信息
        returnmap.put("pnet", autolist.getPnet());
        returnmap.put("cnet", autolist.getCnet());
        returnmap.put("pubMask", autolist.getPubMask());
        returnmap.put("cluMask", autolist.getCluMask());
        returnmap.put("ptNum", autolist.getPtNum());
        returnmap.put("pgNum", autolist.getPgNum());

        return new ResponseResult<Map<String, Object>>(returnmap);
    }

    //集群部署
    public ResponseResult getStartInstall(String token) {
        UserHolder userHolder = UserHolder.getInstance();
        AutoList autolist = userHolder.getAutoMap().get(token);

        // 此阶段结束
        Runnable stateRunnable = new Runnable() {
            @Override
            public void run() {
                userHolder.setRunning(true);
                switch (userHolder.getState()) {
                    case STATE_INIT:
                        GlobalCache_Compile(token);
                        try {
                            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.checkCompile().entrySet()) {
                                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                    userHolder.setState(STATE_COMPILE);
                                    userHolder.setStateNum(userHolder.getStateNum() + 1);
                                } else {
                                    log.info("checkCompile failed.");
                                    UserHolder.getInstance().getAutopipe().add("checkCompile失败");
                                    UserHolder.getInstance().setSuccess(false);
                                    return;
                                }
                            }
                        } catch (GlobalCacheSDKException e) {
                            log.info("checkCompile failed.");
                            System.out.println("clientNodeConfCompileEnv失败");
                            e.printStackTrace();
                            UserHolder.getInstance().setSuccess(false);
                            UserHolder.getInstance().getAutopipe().add("clientNodeConfCompileEnv失败");
                            return;
                        }
                        break;
                    case STATE_COMPILE:
                        GlobalCache_Distribute(token);
                        try {
                            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.checkDistribute().entrySet()) {
                                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                    userHolder.setState(STATE_DISTRIBUTE);
                                    userHolder.setStateNum(userHolder.getStateNum() + 1);
                                } else {
                                    log.info("checkdistribute failed.");
                                    UserHolder.getInstance().getAutopipe().add("checkdistribute 失败");
                                    UserHolder.getInstance().setSuccess(false);
                                    return;
                                }
                            }
                        } catch (GlobalCacheSDKException e) {
                            log.info("checkdistribute failed.");
                            System.out.println("checkdistribute 失败");
                            e.printStackTrace();
                            UserHolder.getInstance().setSuccess(false);
                            UserHolder.getInstance().getAutopipe().add("checkdistribute 失败");
                            return;
                        }
                        break;

                    case STATE_DISTRIBUTE:
                        break;
                    case STATE_CLIENT:
                        break;
                    case STATE_SERVER:
                        break;
                    default:
                        break;
                }
                userHolder.setRunning(false);
            }
        };
        String returnstr = "";
        Thread thread = new Thread(stateRunnable);
        if (userHolder.isRunning() == false) {
            thread.start();
        }
        int len = userHolder.getAutopipe().size();
        for (int i = 0; i < len; ++i) {
            returnstr += userHolder.getAutopipe().poll() + "\n";
        }
        HashMap<String, Object> returnmap = new HashMap<>();
        returnmap.put("installLogInfo", returnstr);
        if (userHolder.isIsdeployfinished()) {
            returnmap.put("isEnd", true);
        } else {
            returnmap.put("isEnd", false);
        }
        Runnable finishRunnable = new Runnable() {
            public void run() {
                String ceph1ip = "";
                List<Ceph> cephs = new ArrayList<>();
                ArrayList<String> hosts = new ArrayList<>();
                for (AutoList.AutoEntity entity : userHolder.getAutoMap().get(token).getAutoEntityArrayList()) {
                    hosts.add(entity.getName());
                    if (entity.getRoleName().equals("ceph1")) {
                        ceph1ip = entity.getName();
                    }
                    if (entity.getRoleName().contains("ceph")) {
                        Ceph ceph = new Ceph(entity.getRoleName(), entity.getName());
                        cephs.add(ceph);
                    }
                }
                // 释放root
                // 1. 清空role table
                // 2. 512个 arraylist<string> 保存
                menuMapper.truncateTable();
                menuMapper.insertCephs(cephs);

                userHolder.setCeph1(ceph1ip);
                regMapper.SetIp(ceph1ip);
                userHolder.setIsdeployfinished(true);

                regMapper.setfinished();
                sessionService.initSession();
                for (int i = 0; i < hosts.size(); i++) {
                    try {
                        GlobalCacheSDK.releaseSession(hosts.get(i), "root");
                    } catch (GlobalCacheSDKException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Thread finishThread = new Thread();
        if (userHolder.isRunning() == false && userHolder.isSuccess()) {
            thread.start();
        }
        returnmap.put("nowStep",userHolder.getStateNum());
        returnmap.put("nowEnd",userHolder.isRunning() == false);
        return new ResponseResult<Map<String, Object>>(returnmap);
    }

    /**
     * 初始化部署配置文件
     * @param token
     */
    void initDeployConf(String token) {
        System.out.println("Call initDeployConf()");

        UserHolder userHolder = UserHolder.getInstance();
        AutoList autolist = userHolder.getAutoMap().get(token);

        ArrayList<CephConf> cephConfs = new ArrayList<>();
        ArrayList<ClientConf> clientConfs = new ArrayList<>();
        ClusterConf clusterConf = new ClusterConf();

        Map<String, ArrayList<String>> dataDiskList = new HashMap<>();
        Map<String, ArrayList<String>> cacheDiskList = new HashMap<>();

        for (AutoList.AutoEntity entity : autolist.getAutoEntityArrayList()) {
            ArrayList<AutoList.AutoEntity.DataDisk> datalist = entity.getDataList();
            ArrayList<String> dataarray = new ArrayList<>();
            for (int i = 0; i < datalist.size(); ++i) {
                dataarray.add(datalist.get(i).getName());
            }
            if (dataarray.size() > 0) {
                dataDiskList.put(entity.getName(), dataarray);
            }
        }
        for (AutoList.AutoEntity entity : autolist.getAutoEntityArrayList()) {
            ArrayList<AutoList.AutoEntity.CacheDisk> cachelist = entity.getCacheList();
            ArrayList<String> cachearray = new ArrayList<>();
            for (int i = 0; i < cachelist.size(); ++i) {
                cachearray.add(cachelist.get(i).getName());
            }
            if (cachearray.size() > 0) {
                cacheDiskList.put(entity.getName(), cachearray);
            }
        }
        String[] pubMask = autolist.getPubMask().split("\\.");
        long pubnum = (Long.parseLong(pubMask[0]) << 24) + (Long.parseLong(pubMask[1]) << 16) + (Long.parseLong(pubMask[2]) << 8) + (Long.parseLong(pubMask[3]));
        int pubonesCount = 0;
        for (int i = 0; i < 32; i++) {
            if ((pubnum & (1 << i)) != 0) {
                pubonesCount++;
            }
        }
        System.out.println("pubCount: " + pubonesCount);


        String[] cluMask = autolist.getCluMask().split("\\.");
        long clunum = (Long.parseLong(cluMask[0]) << 24) + (Long.parseLong(cluMask[1]) << 16) + (Long.parseLong(cluMask[2]) << 8) + (Long.parseLong(cluMask[3]));
        int cluonesCount = 0;
        for (int i = 0; i < 32; i++) {
            if ((clunum & (1 << i)) != 0) {
                cluonesCount++;
            }
        }
        System.out.println("cluCount: " + cluonesCount);

        autolist.setCnet(autolist.getCnet() + "/" + cluonesCount);
        autolist.setPnet(autolist.getPnet() + "/" + pubonesCount);


        System.out.println(autolist.getPnet());
        log.info(autolist.getPnet());
        System.out.println(autolist.getCnet());
        log.info(autolist.getCnet());


        List<AutoList.AutoEntity> autoEntities = autolist.getAutoEntityArrayList();
        int num = 1;
        int name_num = 2;
        String cephname = "ceph";
        ArrayList<String> ceph1ip = new ArrayList<>();
        ArrayList<String> cephips = new ArrayList<>();
        for (AutoList.AutoEntity entity : autoEntities) {
            if (entity.getRoleName().equals("ceph1")) {
                ceph1ip.add(entity.getName());
                CephConf cephConf = new CephConf(cephname + "1", num++, true, true, true, entity.getLocalIPv4(),
                        entity.getName(), entity.getClusterIPv4(), autolist.getPubMask(), autolist.getPassword(),
                        dataDiskList.get(entity.getName()),
                        cacheDiskList.get(entity.getName()));
                cephConfs.add(cephConf);
                cephips.add(entity.getName());
            } else if (entity.getRoleName().contains("ceph")) {
                CephConf cephConf = new CephConf(cephname + name_num, num++, false, false, false, entity.getLocalIPv4(),
                        entity.getName(), entity.getClusterIPv4(), autolist.getPubMask(), autolist.getPassword(),
                        dataDiskList.get(entity.getName()),
                        cacheDiskList.get(entity.getName()));
                cephConfs.add(cephConf);
                ++name_num;
                cephips.add(entity.getName());
            }
        }

        num = 1;
        String clientname = "client";
        ArrayList<String> clienthosts = new ArrayList<>();

        for (AutoList.AutoEntity entity : autoEntities) {
            if (entity.getRoleName().contains("client")) {
                ClientConf clientConf = new ClientConf(clientname + num, autolist.getPubMask(),
                        entity.getName(), autolist.getPassword());
                clientConfs.add(clientConf);
                clienthosts.add(entity.getName());
                ++num;
            }
        }

        clusterConf.setPtNum(autolist.getPtNum());
        clusterConf.setPgNum(autolist.getPgNum());
        clusterConf.setPublicNetwork(autolist.getPnet());
        clusterConf.setClusterNetwork(autolist.getCnet());
        log.info("initClusterSettings start.");
        try {
            GlobalCacheSDK.initClusterSettings(cephConfs, clientConfs, clusterConf);
        } catch (GlobalCacheSDKException e) {
            UserHolder.getInstance().getAutopipe().add("配置文件初始化失败");
            UserHolder.getInstance().setIsdeployfinished(true);
            UserHolder.getInstance().setSuccess(false);
            System.out.println("配置文件初始化失败");
            log.info("initClusterSettings failed");
            e.printStackTrace();
        }
    }

    /**
     * 服务端节点编译相关依赖包
     */
    void compileDependenciesOnServerNode() {
        System.out.println("Call compileDependenciesOnServerNode()");

        // 配置编译环境
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("compileNodeConfEnv", "compile node configure envrionment");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            System.out.println("服务端节点配置编译环境失败");
            e.printStackTrace();
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("服务端节点配置编译环境失败");

            return;
        }

        // 编译软件
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("compileNodeBuildPkgs", "compile node build packages");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            System.out.println("编译软件失败");
            e.printStackTrace();
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("编译软件失败");

            return;
        }
    }

    /**
     * 分发依赖包
     */
    void dependenciesDistribute() {
        System.out.println("Call dependenciesDistribute()");

        // 分发软件包
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("compileNodeDistributePkgs", "compile node distribute packages");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            System.out.println("分发软件包失败");
            e.printStackTrace();
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("分发软件包失败");

            return;
        }
    }

    /**
     * 客户端节点编译相关依赖包
     */
    void compileDependenciesOnClientNode() {
        System.out.println("Call compileDependenciesOnClientNode()");

        // 配置编译环境
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("clientNodeConfCompileEnv", "client node configure compile envrionment");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            System.out.println("客户端节点配置编译环境失败");
            e.printStackTrace();
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("客户端节点配置编译环境失败");

            return;
        }

        // 编译软件
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("clientNodeBuildPkgs", "client node build packages");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            System.out.println("编译软件失败");
            e.printStackTrace();
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("编译软件失败");

            return;
        }
    }

    /**
     * Ceph部署
     */
    void cephDeploy() {
        System.out.println("Call cephDeploy()");

        // 配置Ceph环境
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("allNodeCephConfEnv", "all node configure ceph envrionment");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            System.out.println("配置Ceph环境失败");
            e.printStackTrace();
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("配置Ceph环境失败");

            return;
        }

        // 配置ntp服务端
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("ntpServerNodeConfEnv", "ntp server node configure envrionment");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            System.out.println("配置ntp服务端失败");
            e.printStackTrace();
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("配置ntp服务端失败");

            return;
        }

        // 配置ntp客户端
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("ntpClientNodeConfEnv", "ntp client node configure envrionment");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            System.out.println("配置ntp客户端失败");
            e.printStackTrace();
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("配置ntp客户端失败");

            return;
        }

        // 安装Ceph
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("cephNodeInstallPkgs", "ceph node install packages");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            System.out.println("安装Ceph失败");
            e.printStackTrace();
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("安装Ceph失败");

            return;
        }

        // 部署Ceph
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("cephNodeInstallPkgs", "ceph node install packages");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            System.out.println("安装Ceph失败");
            e.printStackTrace();
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("安装Ceph失败");

            return;
        }
    }

    /**
     * GlobalCache部署
     */
    void gcacheDeploy() {
        System.out.println("Call gcacheDeploy()");

        // 配置服务端gcache环境
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("serverNodeConfEnv", "server node configure globalcache envrionment");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            System.out.println("配置服务端gcache环境失败");
            e.printStackTrace();
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("配置服务端gcache环境失败");

            return;
        }

        // 配置客户端gcache环境
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("clientNodeConfEnv", "client node configure globalcache envrionment");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            System.out.println("配置客户端gcache环境失败");
            e.printStackTrace();
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("配置客户端gcache环境失败");

            return;
        }

        // 服务端安装gcache
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("serverNodeInstallPkgs", "server node install globalcache package");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            System.out.println("服务端安装gcache失败");
            e.printStackTrace();
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("服务端安装gcache失败");

            return;
        }

        // 客户端安装gcache
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("clientNodeInstallPkgs", "client node install globalcache package");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            System.out.println("客户端安装gcache失败");
            e.printStackTrace();
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("客户端安装gcache失败");

            return;
        }
    }

    //集群部署
    public ResponseResult getBeginInstall(String token) {
        UserHolder userHolder = UserHolder.getInstance();
        AutoList autolist = userHolder.getAutoMap().get(token);
        Runnable myRunnable = new Runnable() {
            public void run() {
                // 这部分的代码逻辑？
                initDeployConf(token);
                userHolder.setState(STATE_INIT);
                userHolder.setStateNum(userHolder.getStateNum() + 1);
            }
        };
        Thread thread = new Thread(myRunnable);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        HashMap<String, Object> returnmap = new HashMap<>();
        if (userHolder.getState() == STATE_INIT) {
            returnmap.put("isBegin", true);
        } else {
            returnmap.put("isBegin", false);
        }
        return new ResponseResult<Map<String, Object>>(returnmap);
    }

    /**
     * GlobalCacheSDK异步部署接口调用封装
     *
     * @param asyncMethodName
     * @param banner 后端日志条幅
     * @return
     * @throws GlobalCacheSDKException
     */
    private Map<String, AsyncEntity> asyncDeployMethodCaller(String asyncMethodName, String banner) throws GlobalCacheSDKException {
        Method asyncMethod = null;
        try {
            Class<?> sdkClass = null;
            sdkClass = Class.forName("com.hw.globalcachesdk.GlobalCacheSDK");
            asyncMethod = sdkClass.getMethod(asyncMethodName);
        } catch (ClassNotFoundException e) {
            System.err.println("not found GlobalCacheSDK class");
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            System.err.println("not found " + asyncMethodName + " method in GlobalCacheSDK class");
            throw new RuntimeException(e);
        }

        log.info(banner + " start.");

        Map<String, AsyncEntity> entityMap = new HashMap<>();
        try {
            Map<String, CommandExecuteResult> result = (Map<String, CommandExecuteResult>) asyncMethod.invoke(null);
            for (Map.Entry<String, CommandExecuteResult> entry : result.entrySet()) {
                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                    entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                } else {
                    log.info(banner + " failed.");
                    entityMap.put(entry.getKey(), null);
                }
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.info(banner + " failed.");
            System.out.println("call " + asyncMethodName + " failed!");
            throw new RuntimeException(e);
        }

        log.info(banner + " success.");

        return entityMap;
    }

    /**
     * 打印异步接口控制台输出并等待其执行完毕
     *
     * @param asyncEntityMap 异步调用Entity
     */
    private void printConsoleLogAndWaitAsyncCallFinish(Map<String, AsyncEntity> asyncEntityMap) throws AsyncThreadException {
        int countDown = asyncEntityMap.size();
        while (countDown > 0) {
            for (Map.Entry<String, AsyncEntity> entry : asyncEntityMap.entrySet()) {
                AsyncEntity entity = entry.getValue();
                String line = entity.readLine();
                if (line == null) {
                    entity.waitFinish(); // 此时线程已经读取完毕，关闭缓冲区和Channel
                    countDown -= 1;
                    continue;
                }
                System.out.println(entry.getKey() + ": " + line);
                UserHolder.getInstance().getAutopipe().add(entry.getKey() + ": " + line);
            }
        }
    }
}
