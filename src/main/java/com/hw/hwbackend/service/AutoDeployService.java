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
import com.hw.hwbackend.mapper.RegMapper;
import com.hw.hwbackend.util.ResponseResult;
import com.hw.hwbackend.util.UserHolder;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import static jodd.util.ThreadUtil.sleep;

//自动化部署的业务类
@Service
public class AutoDeployService {

    @Autowired
    private RegMapper regMapper;

    @Autowired
    private SessionService sessionService;
    private static Logger log = LoggerFactory.getLogger(AutoDeployService.class);

    public ResponseResult getReset(String token){

        regMapper.setnofinished();
        UserHolder userHolder = UserHolder.getInstance();
        userHolder.setAutoMap(new ConcurrentHashMap<>());
        userHolder.setSuccess(false);
        userHolder.setAutopipe(new LinkedBlockingQueue<String>());
        userHolder.setCeph1("");
        userHolder.setReady(false);
        userHolder.setRunning(false);

        userHolder.setIprelation(null);

        Map<String, Object> map = new HashMap<>();
        map.put("token",token);
        map.put("isSuccessed",true);
        return new ResponseResult<Map<String, Object>>(map);
    }
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
        UserHolder userHolder = UserHolder.getInstance();
        Map<String, Object> map = new HashMap<>();
        boolean isConnected = false;
        boolean isCpu = true;
        boolean isMemory = true;
        boolean isSave = false;
        //判断连接是否存在
        try {
            isSave = GlobalCacheSDK.isSessionExist(ipAddress, "root");
            if (isSave) {
                isConnected = true;
            }
        } catch (GlobalCacheSDKException e) {
            e.printStackTrace();
        }
        //连接不存在就创建连接
        if (isSave == false) {
            try {
                GlobalCacheSDK.createSession(ipAddress, "root", userHolder.getAutoMap().get(token).getPassword(), 22);
                isConnected = true;
            } catch (GlobalCacheSDKException e) {
                e.printStackTrace();
            }
        } else {
            isConnected = true;
        }
        if (isConnected == false || ipAddress.equals("0.0.0.0") || ipAddress.equals("127.0.0.1")) {
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
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.checkHardware(hosts).entrySet()) {
                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                    entityMap.put(entry.getKey(), (ErrorCodeEntity) entry.getValue().getData());
                } else {
                    System.out.println("接口调用失败");
                }
            }
        } catch (GlobalCacheSDKException e) {
            System.out.println("接口调用失败");
            e.printStackTrace();
        }
        //根据获取的节点状态 设置返回信息
        for (Map.Entry<String, ErrorCodeEntity> entry : entityMap.entrySet()) {
            if (entry.getValue().getErrorCode() == 1) {
                isMemory = false;
            }
            if (entry.getValue().getErrorCode() == 2) {
                isCpu = false;
            }
            if(entry.getValue().getErrorCode() == 3){
                isCpu = false;
                isMemory = false;
            }
        }
        //创建返回信息
        map.put("isValid", isConnected && isMemory && isCpu);
        String reason = "";
        if (!isConnected) {
            reason += "未连接成功 ";
        } else if (!isCpu) {
            reason += "cpu不合法 ";
        } else if (!isMemory) {
            reason += "内存不合法 ";
        }
        map.put("reason", reason);


        String rolename = "";
        if (isConnected && isMemory && isCpu) {
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
                if (userHolder.getAutoMap().get(token).getAutoEntityArrayList().get(i).getRemoteIPv4().equals(ipAddress)) {
                    flag = 1;
                    break;
                }
            }
            if (flag == 0) {
                AutoList.AutoEntity autoEntity = new AutoList.AutoEntity();
                autoEntity.setRemoteIPv4(ipAddress);
                autoEntity.setisConnected(isConnected);
                autoEntity.setisCpu(isCpu);
                autoEntity.setisMemory(isMemory);
                autoEntity.setLocalIPv4(ipAddress);
                autoEntity.setClusterIPv4(ipAddress);
                autoEntity.setPublicIPv4(ipAddress);
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
                if (list.get(i).getRemoteIPv4().equals(ipAddress)) {
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
        return new ResponseResult<Map<String, Object>>(map);
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
                if (ipList.get(i).getRemoteIPv4().equals(jsonArray.get(j).getRemoteIPv4())) {
                    ipList.get(i).setLocalIPv4(jsonArray.get(j).getLocalIPv4());
                    ipList.get(i).setClusterIPv4(jsonArray.get(j).getClusterIPv4());
                    ipList.get(i).setPublicIPv4(jsonArray.get(j).getPublicIPv4());
                }
            }
        }
        //保存
        userHolder.getAutoMap().get(token).setAutoEntityArrayList(ipList);
        HashMap<String, Object> returnmap = new HashMap<>();
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
                if (allEntity.get(i).getRemoteIPv4().equals(entity.getRemoteIPv4())) {
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

    public ResponseResult getLog(Map map) {
        UserHolder userHolder = UserHolder.getInstance();
        String returnstr = "";
        int len = userHolder.getAutopipe().size();
        for (int i = 0; i < len; ++i) {
            if(userHolder.getAutoMap().isEmpty()){
                break;
            }
            if(userHolder.getAutopipe().peek() == "" || userHolder.getAutopipe().peek() == null ){
                continue;
            }
            returnstr += userHolder.getAutopipe().poll() + "\n";
        }
        HashMap<String, Object> returnmap = new HashMap<>();

        returnmap.put("installLogInfo", returnstr);
        returnmap.put("nowStep", userHolder.getStateNum());
        returnmap.put("nowEnd", userHolder.isRunning() == false);
        returnmap.put("nowName", userHolder.getStateMap().get(userHolder.getStateNum()));

        System.out.println("nowStep: " + userHolder.getStateNum());
        System.out.println("nowName: " + userHolder.getStateMap().get(userHolder.getStateNum()));
        returnmap.put("nowSuccess", userHolder.isReady());
        returnmap.put("isEnd", userHolder.isSuccess());
        return new ResponseResult<Map<String, Object>>(returnmap);
    }

    //集群部署
    public ResponseResult getInstall(Map data) {
        String str = JSONObject.toJSONString(data);
        JSONObject jsonobject = JSONObject.parseObject(str);
        String token = (String) jsonobject.get("token");
        Integer nowStep = (Integer) jsonobject.get("nowStep");
        UserHolder userHolder = UserHolder.getInstance();
        HashMap<String, Object> returnmap = new HashMap<>();
        userHolder.setReady(false);
        userHolder.setRunning(false);

        Runnable stateRunnable = new Runnable() {
            @Override
            public void run() {
                userHolder.setRunning(true);
                boolean flag = true;
                AutoList autolist = userHolder.getAutoMap().get(token);
                ArrayList<String> ceph1 = new ArrayList<>();
                ArrayList<String> clients = new ArrayList<>();
                for (AutoList.AutoEntity entity : autolist.getAutoEntityArrayList()) {
                    if (entity.getRoleName().equals("ceph1")) {
                        ceph1.add(entity.getRemoteIPv4());
                    }
                    if (entity.getRoleName().contains("client")) {
                        clients.add(entity.getRemoteIPv4());
                    }
                }
                switch (nowStep) {
                    case 1:
                        initDeployConf(token);
                        try {
                            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.checkConf().entrySet()) {
                                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                    UserHolder.getInstance().getAutopipe().add(entry.getKey() + "运行配置检查脚本");
                                    System.out.println(entry.getKey() + "运行配置检查脚本");
                                    ErrorCodeEntity errorCodeEntity = (ErrorCodeEntity) entry.getValue().getData();
                                    UserHolder.getInstance().getAutopipe().add(errorCodeEntity.getMessage());
                                    if (errorCodeEntity.getErrorCode() != 0) {
                                        flag = false;
                                    }

                                } else {
                                    flag = false;
                                    log.info("checkConf failed.");
                                    UserHolder.getInstance().getAutopipe().add("checkConf失败");
                                    UserHolder.getInstance().setSuccess(false);
                                }
                            }
                        } catch (GlobalCacheSDKException e) {
                            flag = false;
                            log.info("checkConf failed.");
                            System.out.println("checkConf 失败");
                            e.printStackTrace();
                            UserHolder.getInstance().setSuccess(false);
                            UserHolder.getInstance().getAutopipe().add("checkConf 失败");
                        }
                        if (flag) {
                            userHolder.setReady(true);
                        }
                        break;
                    case 2:
                        compileDependenciesOnServerNode(token);

                        try {
                            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.checkCompile(ceph1).entrySet()) {
                                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                    UserHolder.getInstance().getAutopipe().add(entry.getKey() + "运行编译检查脚本");
                                    System.out.println(entry.getKey() + "运行编译检查脚本");
                                    ErrorCodeEntity errorCodeEntity = (ErrorCodeEntity) entry.getValue().getData();
                                    UserHolder.getInstance().getAutopipe().add(errorCodeEntity.getMessage());
                                    if (errorCodeEntity.getErrorCode() != 0) {
                                        System.out.println(entry.getKey() + "编译检查结果");
                                        UserHolder.getInstance().getAutopipe().add(entry.getKey() + "编译检查结果");
                                        flag = false;
                                    }
                                } else {
                                    flag = false;
                                    log.info("checkCompile failed.");
                                    UserHolder.getInstance().getAutopipe().add("checkCompile 失败");
                                    UserHolder.getInstance().setSuccess(false);
                                }
                            }
                        } catch (GlobalCacheSDKException e) {
                            flag = false;
                            log.info("checkCompile failed.");
                            System.out.println("checkCompile 失败");
                            e.printStackTrace();
                            UserHolder.getInstance().setSuccess(false);
                            UserHolder.getInstance().getAutopipe().add("checkCompile 失败");
                        }
                        if (flag) {
                            userHolder.setReady(true);
                        }
                        break;
                    case 3:
                        dependenciesDistribute(token);
                        try {
                            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.checkDistribute().entrySet()) {
                                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                    ErrorCodeEntity errorCodeEntity = (ErrorCodeEntity) entry.getValue().getData();
                                    UserHolder.getInstance().getAutopipe().add(errorCodeEntity.getMessage());
                                    if (errorCodeEntity.getErrorCode() != 0) {
                                        flag = false;
                                    }
                                } else {
                                    flag = false;
                                    log.info("checkDistribute failed.");
                                    UserHolder.getInstance().getAutopipe().add("checkDistribute 失败");
                                    UserHolder.getInstance().setSuccess(false);
                                }
                            }
                        } catch (GlobalCacheSDKException e) {
                            flag = false;
                            log.info("checkDistribute failed.");
                            System.out.println("checkDistribute 失败");
                            e.printStackTrace();
                            UserHolder.getInstance().setSuccess(false);
                            UserHolder.getInstance().getAutopipe().add("checkDistribute 失败");
                        }
                        if (flag) {
                            userHolder.setReady(true);
                        }
                        break;
                    case 4:
                        compileDependenciesOnClientNode(token);
                        try {
                            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.checkCompile(clients).entrySet()) {
                                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {

                                    UserHolder.getInstance().getAutopipe().add(entry.getKey() + "运行compilenode检查脚本");
                                    System.out.println(entry.getKey() + "运行compilenode脚本");
                                    ErrorCodeEntity errorCodeEntity = (ErrorCodeEntity) entry.getValue().getData();
                                    UserHolder.getInstance().getAutopipe().add(errorCodeEntity.getMessage());
                                    if (errorCodeEntity.getErrorCode() != 0) {
                                        UserHolder.getInstance().getAutopipe().add(entry.getKey() + "运行compilenode检查脚本失败");
                                        System.out.println(entry.getKey() + "运行compilenode脚本失败");
                                        flag = false;
                                    }
                                } else {
                                    flag = false;
                                    log.info("checkClient failed.");
                                    UserHolder.getInstance().getAutopipe().add("checkClient 失败");
                                    UserHolder.getInstance().setSuccess(false);
                                }
                            }
                        } catch (GlobalCacheSDKException e) {
                            flag = false;
                            log.info("checkClient failed.");
                            System.out.println("checkClient 失败");
                            e.printStackTrace();
                            UserHolder.getInstance().setSuccess(false);
                            UserHolder.getInstance().getAutopipe().add("checkClient 失败");
                        }
                        if (flag) {
                            userHolder.setReady(true);
                        }
                        break;
                    case 5:
                        cephDeploy(token);
                        try {
                            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.checkCeph().entrySet()) {
                                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                    UserHolder.getInstance().getAutopipe().add(entry.getKey() + "运行cephDeploy检查脚本");
                                    System.out.println(entry.getKey() + "运行cephDeploy脚本");
                                    ErrorCodeEntity errorCodeEntity = (ErrorCodeEntity) entry.getValue().getData();
                                    UserHolder.getInstance().getAutopipe().add(errorCodeEntity.getMessage());
                                    if (errorCodeEntity.getErrorCode() != 0) {
                                        UserHolder.getInstance().getAutopipe().add(entry.getKey() + "运行cephDeploy检查脚本失败");
                                        System.out.println(entry.getKey() + "运行cephDeploy脚本失败");
                                        flag = false;
                                    }
                                } else {
                                    flag = false;
                                    log.info("checkCeph failed.");
                                    UserHolder.getInstance().getAutopipe().add("checkCeph失败");
                                    UserHolder.getInstance().setSuccess(false);
                                }
                            }
                        } catch (GlobalCacheSDKException e) {
                            flag = false;
                            log.info("checkCeph failed.");
                            System.out.println("checkCeph 失败");
                            e.printStackTrace();
                            UserHolder.getInstance().setSuccess(false);
                            UserHolder.getInstance().getAutopipe().add("checkCeph失败");
                        }
                        if (flag) {
                            userHolder.setReady(true);
                        }
                        break;
                    case 6:
                        gcacheDeploy(token);
                        boolean bclient = true;
                        boolean bserver = true;
                        try {
                            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.checkServer().entrySet()) {
                                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                    ErrorCodeEntity errorCodeEntity = (ErrorCodeEntity) entry.getValue().getData();
                                    UserHolder.getInstance().getAutopipe().add(errorCodeEntity.getMessage());
                                    if (errorCodeEntity.getErrorCode() != 0) {
                                        bserver = false;
                                    }
                                } else {
                                    bserver = false;
                                    log.info("checkServer failed.");
                                    UserHolder.getInstance().getAutopipe().add("checkServer 失败");
                                    UserHolder.getInstance().setSuccess(false);
                                }
                            }

                            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.checkClient().entrySet()) {
                                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                    ErrorCodeEntity errorCodeEntity = (ErrorCodeEntity) entry.getValue().getData();
                                    UserHolder.getInstance().getAutopipe().add(errorCodeEntity.getMessage());
                                    if (errorCodeEntity.getErrorCode() != 0) {
                                        bclient = false;
                                    }
                                } else {
                                    bclient = false;
                                    log.info("checkClient failed.");
                                    UserHolder.getInstance().getAutopipe().add("checkClient 失败");
                                    UserHolder.getInstance().setSuccess(false);
                                }
                            }
                        } catch (GlobalCacheSDKException e) {
                            log.info("checkServer or checkClient failed.");
                            System.out.println("checkServer or checkClient 失败");
                            bclient = false;
                            bserver = false;
                            e.printStackTrace();
                            UserHolder.getInstance().setSuccess(false);
                            UserHolder.getInstance().getAutopipe().add("checkServer or checkClient 失败");
                        }
                        if (bserver && bclient) {
                            userHolder.setReady(true);
                        }

                        break;
                    case 7:
                        gcacheInit(token);
                        UserHolder.getInstance().getAutopipe().add("Global Cache 初始化中...");
                        int times = 5;
                        boolean isCacheRunning = true;
                        for(int i = 0;i < times;++i){
                            UserHolder.getInstance().getAutopipe().add("正在重新尝试: " + (i+1) + "/" + times + "...");
                            flag = false;
                            try {
                                for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.checkGlobalCacheRunning().entrySet()) {
                                    if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                        ErrorCodeEntity errorCodeEntity = (ErrorCodeEntity) entry.getValue().getData();
                                        UserHolder.getInstance().getAutopipe().add(errorCodeEntity.getMessage());
                                        if (errorCodeEntity.getErrorCode() != 0) {
                                            isCacheRunning = true;
                                        }else{
                                            isCacheRunning = false;
                                        }
                                    } else {
                                        isCacheRunning = true;
                                        log.info("checkGlobalCacheRunning failed.");
                                        UserHolder.getInstance().getAutopipe().add("checkGlobalCacheRunning 失败");
                                        UserHolder.getInstance().setSuccess(false);
                                    }
                                }
                            } catch (GlobalCacheSDKException e) {
                                e.printStackTrace(System.out);
                                
                                log.info("checkGlobalCacheRunning failed.");
                                System.out.println("checkGlobalCacheRunning 失败");
                                isCacheRunning = true;
                                UserHolder.getInstance().setSuccess(false);
                                UserHolder.getInstance().getAutopipe().add("checkGlobalCacheRunning 失败");
                            }
                            if(isCacheRunning == false){
                                flag = true;
                                break;
                            }
                            sleep(1000 * 120); // wait 120s
                        }
                        if (flag) {
                            userHolder.setCeph1("");
                            userHolder.setSuccess(true);
                            userHolder.setReady(true);
                        }
                        break;

                }
                userHolder.setRunning(false);
            }
        };
        Thread statethread = new Thread(stateRunnable);
        statethread.start();
        try {
            statethread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        userHolder.setRunning(false);

        if(userHolder.isReady()){
            userHolder.setStateNum(nowStep + 1);
        }
        returnmap.put("nowName", userHolder.getStateMap().get(userHolder.getStateNum()));
        Runnable finishRunnable = new Runnable() {
            public void run() {
                System.out.println("线程开始运行");
                regMapper.setfinished();
                // 必须保证数据库写入完成
                sleep(500);
                sessionService.initSession();
            }
        };
        Thread finishThread = new Thread(finishRunnable);
        if (userHolder.isSuccess()) {
            System.out.println("wait 60s...");
            UserHolder.getInstance().getAutopipe().add("正在收集数据, 请等待...");
            finishThread.start();
        }
        return new ResponseResult<Map<String, Object>>(returnmap);
    }

    /**
     * 初始化部署配置文件
     *
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
                dataDiskList.put(entity.getRemoteIPv4(), dataarray);
            }
        }
        for (AutoList.AutoEntity entity : autolist.getAutoEntityArrayList()) {
            ArrayList<AutoList.AutoEntity.CacheDisk> cachelist = entity.getCacheList();
            ArrayList<String> cachearray = new ArrayList<>();
            for (int i = 0; i < cachelist.size(); ++i) {
                cachearray.add(cachelist.get(i).getName());
            }
            if (cachearray.size() > 0) {
                cacheDiskList.put(entity.getRemoteIPv4(), cachearray);
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

        // 第一次加/ 后面调用的时候把/后面的Count替换一下
        if(autolist.getCnet().contains("/")){
            autolist.setPnet(autolist.getCnet().substring(0, autolist.getPnet().indexOf("/")+1) + cluonesCount);
        }else{
            autolist.setCnet(autolist.getCnet() + "/" + cluonesCount);
        }
        if(autolist.getPnet().contains("/")){
            autolist.setCnet(autolist.getPnet().substring(0, autolist.getCnet().indexOf("/")+1) + pubonesCount);
        }else{
            autolist.setPnet(autolist.getPnet() + "/" + pubonesCount);
        }


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
                ceph1ip.add(entity.getRemoteIPv4());
                CephConf cephConf = new CephConf(cephname + "1", num++, true, true, true,entity.getRemoteIPv4(), entity.getLocalIPv4(),
                        entity.getPublicIPv4(), entity.getClusterIPv4(), autolist.getPubMask(), autolist.getPassword(),
                        dataDiskList.get(entity.getRemoteIPv4()),
                        cacheDiskList.get(entity.getRemoteIPv4()));
                cephConfs.add(cephConf);
                cephips.add(entity.getRemoteIPv4());
            } else if (entity.getRoleName().contains("ceph")) {
                CephConf cephConf = new CephConf(cephname + name_num, num++, false, false, false,entity.getRemoteIPv4(), entity.getLocalIPv4(),
                        entity.getPublicIPv4(), entity.getClusterIPv4(), autolist.getPubMask(), autolist.getPassword(),
                        dataDiskList.get(entity.getRemoteIPv4()),
                        cacheDiskList.get(entity.getRemoteIPv4()));
                cephConfs.add(cephConf);
                ++name_num;
                cephips.add(entity.getRemoteIPv4());
            }
        }

        num = 1;
        String clientname = "client";
        ArrayList<String> clienthosts = new ArrayList<>();

        for (AutoList.AutoEntity entity : autoEntities) {
            if (entity.getRoleName().contains("client")) {
                ClientConf clientConf = new ClientConf(clientname + num, autolist.getPubMask(),entity.getRemoteIPv4(),
                        entity.getPublicIPv4(), autolist.getPassword());
                clientConfs.add(clientConf);
                clienthosts.add(entity.getRemoteIPv4());
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
            System.out.println("初始化完成------------------------");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            UserHolder.getInstance().getAutopipe().add("配置文件初始化失败");
            UserHolder.getInstance().setSuccess(false);
            System.out.println("配置文件初始化失败");
            log.info("initClusterSettings failed");
        }
        System.out.println("初始化完成");
    }

    /**
     * 服务端节点编译相关依赖包
     */
    void compileDependenciesOnServerNode(String token) {
        System.out.println("Call compileDependenciesOnServerNode()");

        // 配置编译环境
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("compileNodeConfEnv", "compile node configure envrionment");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("服务端节点配置编译环境失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("服务端节点配置编译环境失败");

            return;
        }

        // 编译软件
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("compileNodeBuildPkgs", "compile node build packages");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("编译软件失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("编译软件失败");

            return;
        }
    }

    /**
     * 分发依赖包
     */
    void dependenciesDistribute(String token) {
        System.out.println("Call dependenciesDistribute()");

        // 分发软件包
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("compileNodeDistributePkgs", "compile node distribute packages");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("分发软件包失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("分发软件包失败");

            return;
        }
    }

    /**
     * 客户端节点编译相关依赖包
     */
    void compileDependenciesOnClientNode(String token) {
        System.out.println("Call compileDependenciesOnClientNode()");

        // 配置编译环境
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("clientNodeConfCompileEnv", "client node configure compile envrionment");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("客户端节点配置编译环境失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("客户端节点配置编译环境失败");

            return;
        }

        // 编译软件
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("clientNodeBuildPkgs", "client node build packages");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("编译软件失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("编译软件失败");

            return;
        }
    }

    /**
     * Ceph部署
     */
    void cephDeploy(String token) {
        System.out.println("Call cephDeploy()");

        // Ceph节点配置Ceph环境
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("cephNodeConfEnv", "ceph node configure ceph envrionment");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("配置Ceph环境失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("配置Ceph环境失败");

            return;
        }

        // Client节点配置Ceph环境
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("clientNodeInstallCeph", "client node configure ceph envrionment");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("配置Ceph环境失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("配置Ceph环境失败");

            return;
        }

        // 配置ntp服务端
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("ntpServerNodeConfEnv", "ntp server node configure envrionment");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("配置ntp服务端失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("配置ntp服务端失败");

            return;
        }

        // 配置ntp客户端
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("ntpClientNodeConfEnv", "ntp client node configure envrionment");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("配置ntp客户端失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("配置ntp客户端失败");

            return;
        }

        // 清理OSD
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("ceph1NodeCleanOsd", "ceph1 node clean osd");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("清理OSD失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("清理OSD失败");

            return;
        }

        // 卸载globalcache服务端
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("serverNodeUninstall", "server node uninstall globalcache");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("卸载globalcache服务端失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("卸载globalcache服务端失败");

            return;
        }

        // Ceph磁盘分区
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("cephNodeDiskPartition", "ceph node disk partition");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("格式化磁盘失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("格式化磁盘失败");
            return;
        }

        // 部署Ceph
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("ceph1NodeDeployCeph", "ceph1 node deploy ceph");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("部署Ceph失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("部署Ceph失败");
            return;
        }
    }

    /**
     * GlobalCache部署
     */
    void gcacheDeploy(String token) {
        System.out.println("Call gcacheDeploy()");

        // 配置服务端gcache环境
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("serverNodeConfEnv", "server node configure globalcache envrionment");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("配置服务端gcache环境失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("配置服务端gcache环境失败");

            return;
        }

        // 配置客户端gcache环境
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("clientNodeConfEnv", "client node configure globalcache envrionment");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("配置客户端gcache环境失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("配置客户端gcache环境失败");

            return;
        }

        // 服务端安装gcache
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("serverNodeInstallPkgs", "server node install globalcache package");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("服务端安装gcache失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("服务端安装gcache失败");

            return;
        }

        // 客户端安装gcache
        try {
            Map<String, AsyncEntity> asyncEntityMap = asyncDeployMethodCaller("clientNodeInstallPkgs", "client node install globalcache package");
            printConsoleLogAndWaitAsyncCallFinish(asyncEntityMap);
        } catch (GlobalCacheSDKException | AsyncThreadException e) {
            e.printStackTrace(System.out);
            System.out.println("客户端安装gcache失败");
            UserHolder.getInstance().setSuccess(false);
            UserHolder.getInstance().getAutopipe().add("客户端安装gcache失败");

            return;
        }
    }

    void gcacheInit(String token) {
        System.out.println("Call gcacheInit()");
        UserHolder userHolder = UserHolder.getInstance();
        AutoList autolist = userHolder.getAutoMap().get(token);
        ArrayList<String> cephips = new ArrayList<>();
        for (AutoList.AutoEntity entity : autolist.getAutoEntityArrayList()) {
            if (entity.getRoleName().equals("ceph1")) {
                cephips.add(entity.getRemoteIPv4());
            } else if (entity.getRoleName().contains("ceph")) {
                cephips.add(entity.getRemoteIPv4());
            }
        }
        Map<String, AsyncEntity> entityMap = new HashMap<>(cephips.size());
        try {
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.gcServiceControl(cephips, "start").entrySet()) {
                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                    entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                } else {
                    log.info("gcServiceControl failed.");
                    UserHolder.getInstance().getAutopipe().add("初始化globalcache失败");
                    System.out.println("初始化globalcache失败");
                    UserHolder.getInstance().setSuccess(false);
                    return;
                }
            }
        } catch (GlobalCacheSDKException e) {
            e.printStackTrace(System.out);
            log.info("gcServiceControl failed.");
            UserHolder.getInstance().getAutopipe().add("初始化globalcache失败");
            UserHolder.getInstance().setSuccess(false);
            System.out.println("初始化globalcache失败");
            return;
        }

        log.info("gcServiceControl success.");
        System.out.println("初始化globalcache成功");
        int countDown = entityMap.size();
        while (countDown > 0) {
            for (Map.Entry<String, AsyncEntity> entry : entityMap.entrySet()) {
                AsyncEntity entity = entry.getValue();
                try {
                    String line = entity.readLine();
                    if (line == null) {
                        entity.waitFinish(); // 此时线程已经读取完毕，关闭缓冲区和Channel
                        countDown -= 1;
                        continue;
                    }
                    UserHolder.getInstance().getAutopipe().add(entry.getKey() + ": " + line);
                    System.out.println(entry.getKey() + ": " + line);
                } catch (AsyncThreadException e) {
                    System.err.println("异步线程异常");
                }
            }
        }

    }

    /**
     * GlobalCacheSDK异步部署接口调用封装
     *
     * @param asyncMethodName
     * @param banner          后端日志条幅
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
        } catch(Exception e) {
            InvocationTargetException targetEx = (InvocationTargetException)e;
            Throwable throwEx = targetEx .getTargetException();
            throw new GlobalCacheSDKException(throwEx.getMessage());
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
                    System.out.println("start wait finish");
                    entity.waitFinish(); // 此时线程已经读取完毕，关闭缓冲区和Channel
                    System.out.println("end wait finish");
                    countDown -= 1;
                    continue;
                }
                System.out.println(entry.getKey() + ": " + line);
                UserHolder.getInstance().getAutopipe().add(entry.getKey() + ": " + line);
            }
        }
    }
}
