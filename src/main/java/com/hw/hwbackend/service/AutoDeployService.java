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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hw.globalcachesdk.ExecuteNode.CEPH1_ONLY;
import static com.hw.globalcachesdk.ExecuteNode.ALL_CLIENT_NODES;

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
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.checkHardware(hosts).entrySet()) {
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
        }else{
            rolename = "";
        }
        boolean ceph = false;
        boolean ceph1 = false;
        boolean client = false;
        map.put("rolename", rolename);
        if(rolename.equals("ceph1")){
            ceph1 = true;
            map.put("ceph",false);
            map.put("ceph1",true);
            map.put("client",false);
        }
        else if(rolename.contains("ceph")){
            ceph = true;
            map.put("ceph",true);
            map.put("ceph1",false);
            map.put("client",false);
        }
        else {
            client = true;
            map.put("ceph",false);
            map.put("ceph1",false);
            map.put("client",false);
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
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.checkHardware(hosts).entrySet()) {
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
//                        System.out.println(entry.getValue().getStatusCode());
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
        autolist.setNetMask((String) jsonobject.get("netMask"));
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
        returnmap.put("netMask", autolist.getNetMask());
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
        returnmap.put("netMask", autolist.getNetMask());
        returnmap.put("ptNum", autolist.getPtNum());
        returnmap.put("pgNum", autolist.getPgNum());

        return new ResponseResult<Map<String, Object>>(returnmap);
    }

    //集群部署
    public ResponseResult getStartInstall(String token) {
        UserHolder userHolder = UserHolder.getInstance();
        String returnstr = "";
        int len = userHolder.getAutopipe().size();
        for (int i = 0; i < len; ++i) {
            returnstr += userHolder.getAutopipe().poll() + "|";
        }
        HashMap<String, Object> returnmap = new HashMap<>();
        returnmap.put("installLogInfo", returnstr);
        if (userHolder.isIsdeployfinished()) {
            returnmap.put("isEnd", true);
        } else {
            returnmap.put("isEnd", false);
        }
        Runnable myRunnable = new Runnable() {
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
        Thread thread = new Thread(myRunnable);
        if (userHolder.isIsdeployfinished() && userHolder.isSuccess()) {
            thread.start();
        }


        return new ResponseResult<Map<String, Object>>(returnmap);
    }

    //集群部署
    public ResponseResult getBeginInstall(String token) {
        Runnable myRunnable = new Runnable() {
            public void run() {
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
                    if (dataarray.size() > 0)
                        dataDiskList.put(entity.getName(), dataarray);
                }
                for (AutoList.AutoEntity entity : autolist.getAutoEntityArrayList()) {
                    ArrayList<AutoList.AutoEntity.CacheDisk> cachelist = entity.getCacheList();
                    ArrayList<String> cachearray = new ArrayList<>();
                    for (int i = 0; i < cachelist.size(); ++i) {
                        cachearray.add(cachelist.get(i).getName());
                    }
                    if (cachearray.size() > 0)
                        cacheDiskList.put(entity.getName(), cachearray);
                }
                String[] ip = autolist.getNetMask().split("\\.");
                long ipnum = (Long.parseLong(ip[0]) << 24) + (Long.parseLong(ip[1]) << 16) + (Long.parseLong(ip[2]) << 8) + (Long.parseLong(ip[3]));
                int onesCount = 0;
                for (int i = 0; i < 32; i++) {
                    if ((ipnum & (1 << i)) != 0) {
                        onesCount++;
                    }
                }
                System.out.println("Count: " + onesCount);
                autolist.setCnet(autolist.getCnet() + "/" + onesCount);
                autolist.setPnet(autolist.getPnet() + "/" + onesCount);
                System.out.println(autolist.getPnet());
                System.out.println(autolist.getCnet());


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
                                entity.getName(), entity.getClusterIPv4(), autolist.getNetMask(), autolist.getPassword(),
                                dataDiskList.get(entity.getName()),
                                cacheDiskList.get(entity.getName()));
                        cephConfs.add(cephConf);
                        cephips.add(entity.getName());
                    } else if (entity.getRoleName().contains("ceph")) {
                        CephConf cephConf = new CephConf(cephname + name_num, num++, false, false, false, entity.getLocalIPv4(),
                                entity.getName(), entity.getClusterIPv4(), autolist.getNetMask(), autolist.getPassword(),
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
                        ClientConf clientConf = new ClientConf(clientname + num, autolist.getNetMask(),
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
                    return;
                }

                log.info("initClusterSettings success");
                UserHolder.getInstance().getAutopipe().add("配置文件初始化成功");
                System.out.println("配置文件初始化成功");
                ArrayList<String> hosts = new ArrayList<>();

                for (AutoList.AutoEntity entity : autoEntities) {
                    hosts.add(entity.getName());
                }

                log.info("compileNodeConfEnv start.");
                // 配置编译节点
                {
                    Map<String, AsyncEntity> entityMap = new HashMap<>(hosts.size());
                    try {
                        for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.compileNodeConfEnv().entrySet()) {
                            if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                            } else {
                                log.info("compileNodeConfEnv failed.");
                                UserHolder.getInstance().getAutopipe().add("配置编译节点失败");
                                UserHolder.getInstance().setIsdeployfinished(true);
                                UserHolder.getInstance().setSuccess(false);
                                System.out.println("配置编译节点失败");
                                return;
                            }
                        }
                    } catch (GlobalCacheSDKException e) {
                        log.info("compileNodeConfEnv failed.");
                        UserHolder.getInstance().getAutopipe().add("配置编译节点失败");
                        UserHolder.getInstance().setIsdeployfinished(true);
                        UserHolder.getInstance().setSuccess(false);
                        System.out.println("配置编译节点失败");
                        e.printStackTrace();
                        return;
                    }

                    log.info("compileNodeConfEnv success.");
                    System.out.println("配置编译节点成功");
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
                                // 将数据发送给前端s
                                UserHolder.getInstance().getAutopipe().add(entry.getKey() + ": " + line);
                                System.out.println(entry.getKey() + ": " + line);
                            } catch (AsyncThreadException e) {
                                System.err.println("异步线程异常");
                            }
                        }
                    }
                }

                log.info("compileNodeBuildPkgs start.");
                // 编译软件
                {
                    Map<String, AsyncEntity> entityMap = new HashMap<>(hosts.size());
                    try {
                        for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.compileNodeBuildPkgs().entrySet()) {
                            if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                            } else {
                                log.info("compileNodeBuildPkgs failed.");
                                UserHolder.getInstance().getAutopipe().add("编译软件失败");
                                UserHolder.getInstance().setIsdeployfinished(true);
                                UserHolder.getInstance().setSuccess(false);
                                return;
                            }
                        }
                    } catch (GlobalCacheSDKException e) {
                        log.info("compileNodeBuildPkgs failed.");
                        System.out.println("编译软件失败");
                        e.printStackTrace();
                        UserHolder.getInstance().setSuccess(false);
                        UserHolder.getInstance().getAutopipe().add("编译软件失败");
                        UserHolder.getInstance().setIsdeployfinished(true);
                        return;
                    }

                    log.info("compileNodeBuildPkgs success.");
                    System.out.println("编译软件成功");
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

                log.info("compileNodeDistributePkgs start.");
                // 分发软件包
                {
                    Map<String, AsyncEntity> entityMap = new HashMap<>(hosts.size());
                    try {
                        for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.compileNodeDistributePkgs().entrySet()) {
                            if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                            } else {
                                log.info("compileNodeDistributePkgs failed.");
                                UserHolder.getInstance().getAutopipe().add("分发软件包失败");
                                UserHolder.getInstance().setIsdeployfinished(true);
                                UserHolder.getInstance().setSuccess(false);
                                return;
                            }
                        }
                    } catch (GlobalCacheSDKException e) {
                        log.info("compileNodeDistributePkgs failed.");
                        System.out.println("分发软件包失败");
                        e.printStackTrace();
                        UserHolder.getInstance().setSuccess(false);
                        UserHolder.getInstance().getAutopipe().add("分发软件包失败");
                        UserHolder.getInstance().setIsdeployfinished(true);
                        return;
                    }

                    log.info("compileNodeDistributePkgs success.");
                    System.out.println("分发软件包成功");

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
                                System.out.println(entry.getKey() + ": " + line);
                                UserHolder.getInstance().getAutopipe().add(entry.getKey() + ": " + line);
                            } catch (AsyncThreadException e) {
                                System.err.println("异步线程异常");
                            }
                        }
                    }
                }

                log.info("clientNodeConfCompileEnv start.");
                // 编译环境配置
                {
                    Map<String, AsyncEntity> entityMap = new HashMap<>(hosts.size());
                    try {
                        for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.clientNodeConfCompileEnv().entrySet()) {
                            if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                            } else {
                                log.info("clientNodeConfCompileEnv failed.");
                                UserHolder.getInstance().getAutopipe().add("clientNodeConfCompileEnv失败");
                                UserHolder.getInstance().setIsdeployfinished(true);
                                UserHolder.getInstance().setSuccess(false);
                                return;
                            }
                        }
                    } catch (GlobalCacheSDKException e) {
                        log.info("clientNodeConfCompileEnv failed.");
                        System.out.println("clientNodeConfCompileEnv失败");
                        e.printStackTrace();
                        UserHolder.getInstance().setSuccess(false);
                        UserHolder.getInstance().getAutopipe().add("clientNodeConfCompileEnv失败");
                        UserHolder.getInstance().setIsdeployfinished(true);
                        return;
                    }

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
//                                System.out.println(entry.getKey() + ": " + line);
                                UserHolder.getInstance().getAutopipe().add(entry.getKey() + ": " + line);
                            } catch (AsyncThreadException e) {
                                System.err.println("异步线程异常");
                            }
                        }
                    }

                    log.info("clientNodeConfCompileEnv success.");
                    UserHolder.getInstance().getAutopipe().add("clientNodeConfCompileEnv success");
                }

                log.info("clientNodeBuildPkgs start.");
                // 客户端节点编译
                {

                    Map<String, AsyncEntity> entityMap = new HashMap<>(hosts.size());
                    try {
                        for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.clientNodeBuildPkgs().entrySet()) {
                            if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                            } else {
                                log.info("clientNodeBuildPkgs failed.");
                                UserHolder.getInstance().getAutopipe().add("clientNodeBuildPkgs失败");
                                UserHolder.getInstance().setIsdeployfinished(true);
                                UserHolder.getInstance().setSuccess(false);
                                return;
                            }
                        }
                    } catch (GlobalCacheSDKException e) {
                        log.info("clientNodeBuildPkgs failed.");
                        System.out.println("clientNodeBuildPkgs失败");
                        e.printStackTrace();
                        UserHolder.getInstance().setSuccess(false);
                        UserHolder.getInstance().getAutopipe().add("clientNodeBuildPkgs失败");
                        UserHolder.getInstance().setIsdeployfinished(true);
                        return;
                    }

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
//                                System.out.println(entry.getKey() + ": " + line);
                                UserHolder.getInstance().getAutopipe().add(entry.getKey() + ": " + line);
                            } catch (AsyncThreadException e) {
                                System.err.println("异步线程异常");
                            }
                        }
                    }
                    log.info("clientNodeBuildPkgs success.");
                    UserHolder.getInstance().getAutopipe().add("clientNodeBuildPkgs success");

                }

                log.info("allNodeCephConfEnv start.");
                // 配置Ceph基础环境
                {
                    Map<String, AsyncEntity> entityMap = new HashMap<>(hosts.size());
                    try {
                        for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.allNodeCephConfEnv().entrySet()) {
                            if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                            } else {
                                log.info("allNodeCephConfEnv failed.");
                                UserHolder.getInstance().getAutopipe().add("配置ceph基础环境失败");
                                UserHolder.getInstance().setIsdeployfinished(true);
                                UserHolder.getInstance().setSuccess(false);
                                return;
                            }
                        }
                    } catch (GlobalCacheSDKException e) {
                        log.info("allNodeCephConfEnv failed.");
                        UserHolder.getInstance().getAutopipe().add("配置ceph基础环境失败");
                        UserHolder.getInstance().setIsdeployfinished(true);
                        UserHolder.getInstance().setSuccess(false);
                        System.out.println("配置ceph基础环境失败");
                        e.printStackTrace();
                        return;
                    }

                    log.info("allNodeCephConfEnv success.");
                    System.out.println("配置ceph基础环境成功");
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
//                                System.out.println(entry.getKey() + ": " + line);
                            } catch (AsyncThreadException e) {
                                System.err.println("异步线程异常");
                            }
                        }
                    }
                }

                log.info("ntpServerNodeConfEnv start.");
                // 配置ntp服务端
                {
                    Map<String, AsyncEntity> entityMap = new HashMap<>(hosts.size());
                    try {
                        for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.ntpServerNodeConfEnv().entrySet()) {
                            if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                            } else {
                                log.info("ntpServerNodeConfEnv failed.");
                                UserHolder.getInstance().getAutopipe().add("配置ntp服务端失败");
                                UserHolder.getInstance().setIsdeployfinished(true);
                                UserHolder.getInstance().setSuccess(false);
                                return;
                            }
                        }
                    } catch (GlobalCacheSDKException e) {
                        log.info("ntpServerNodeConfEnv failed.");
                        UserHolder.getInstance().getAutopipe().add("配置ntp服务端失败");
                        UserHolder.getInstance().setIsdeployfinished(true);
                        System.out.println("配置ntp服务端失败");
                        UserHolder.getInstance().setSuccess(false);
                        e.printStackTrace();
                        return;
                    }

                    log.info("ntpServerNodeConfEnv success.");
                    System.out.println("配置ntp服务端成功");
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
//                                System.out.println(entry.getKey() + ": " + line);
                            } catch (AsyncThreadException e) {
                                System.err.println("异步线程异常");
                            }
                        }
                    }
                }

                log.info("ntpClientNodeConfEnv start.");
                // 配置ntp客户端
                {
                    Map<String, AsyncEntity> entityMap = new HashMap<>(hosts.size());
                    try {
                        for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.ntpClientNodeConfEnv().entrySet()) {
                            if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                            } else {

                                log.info("ntpClientNodeConfEnv failed.");
                                UserHolder.getInstance().getAutopipe().add("配置ntp客户端失败");
                                UserHolder.getInstance().setIsdeployfinished(true);
                                System.out.println("配置ntp客户端失败");
                                UserHolder.getInstance().setSuccess(false);
                                return;
                            }
                        }
                    } catch (GlobalCacheSDKException e) {
                        e.printStackTrace();
                        log.info("ntpClientNodeConfEnv failed.");
                        UserHolder.getInstance().getAutopipe().add("配置ntp客户端失败");
                        UserHolder.getInstance().setIsdeployfinished(true);
                        UserHolder.getInstance().setSuccess(false);
                        System.out.println("配置ntp客户端失败");
                        return;
                    }

                    log.info("ntpClientNodeConfEnv success.");
                    System.out.println("配置ntp客户端成功");
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
//                                System.out.println(entry.getKey() + ": " + line);
                                UserHolder.getInstance().getAutopipe().add(entry.getKey() + ": " + line);
                            } catch (AsyncThreadException e) {
                                System.err.println("异步线程异常");
                            }
                        }
                    }
                }

                log.info("cephNOdeInstallPkgs start.");
                // 安装Ceph
                {
                    Map<String, AsyncEntity> entityMap = new HashMap<>(hosts.size());
                    try {
                        for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.cephNodeInstallPkgs().entrySet()) {
                            if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                            } else {
                                log.info("cephNOdeInstallPkgs failed.");
                                UserHolder.getInstance().setSuccess(false);
                                UserHolder.getInstance().getAutopipe().add("安装ceph失败");
                                UserHolder.getInstance().setIsdeployfinished(true);
                                System.out.println("安装ceph失败");
                                return;

                            }
                        }
                    } catch (GlobalCacheSDKException e) {
                        e.printStackTrace();
                        log.info("cephNOdeInstallPkgs failed.");
                        UserHolder.getInstance().setSuccess(false);
                        UserHolder.getInstance().getAutopipe().add("安装ceph失败");
                        UserHolder.getInstance().setIsdeployfinished(true);
                        System.out.println("安装ceph失败");
                        return;

                    }
                    log.info("cephNOdeInstallPkgs success.");
                    System.out.println("安装ceph成功");
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
//                                System.out.println(entry.getKey() + ": " + line);
                                UserHolder.getInstance().getAutopipe().add(entry.getKey() + ": " + line);
                            } catch (AsyncThreadException e) {
                                System.err.println("异步线程异常");
                            }
                        }
                    }
                }

                log.info("ceph1NodeDeployCeph start.");
                // 部署Ceph
                {
                    Map<String, AsyncEntity> entityMap = new HashMap<>(hosts.size());
                    try {
                        for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.ceph1NodeDeployCeph().entrySet()) {
                            if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                            } else {
                                log.info("ceph1NodeDeployCeph failed.");
                                UserHolder.getInstance().getAutopipe().add("部署ceph失败");
                                UserHolder.getInstance().setSuccess(false);
                                UserHolder.getInstance().setIsdeployfinished(true);
                                System.out.println("部署cpeh失败");
                                return;
                            }
                        }
                    } catch (GlobalCacheSDKException e) {
                        e.printStackTrace();
                        log.info("ceph1NodeDeployCeph failed.");
                        UserHolder.getInstance().getAutopipe().add("部署ceph失败");
                        UserHolder.getInstance().setIsdeployfinished(true);
                        UserHolder.getInstance().setSuccess(false);
                        System.out.println("部署ceph失败");
                        return;

                    }
                    log.info("ceph1NodeDeployCeph success.");
                    System.out.println("部署ceph成功");
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
//                                System.out.println(entry.getKey() + ": " + line);
                            } catch (AsyncThreadException e) {
                                System.err.println("异步线程异常");
                            }
                        }
                    }
                }

                System.out.println("client节点部署成功");

                log.info("serverNodeConfEnv start.");
                // 配置GlobalCache服务端环境
                {
                    Map<String, AsyncEntity> entityMap = new HashMap<>(hosts.size());
                    try {
                        for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.serverNodeConfEnv().entrySet()) {
                            if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                            } else {
                                log.info("serverNodeConfEnv failed.");
                                UserHolder.getInstance().setSuccess(false);
                                UserHolder.getInstance().getAutopipe().add("配置globalcache服务端环境失败");
                                UserHolder.getInstance().setIsdeployfinished(true);
                                System.out.println("配置globalcache服务端环境失败");
                                return;

                            }
                        }
                    } catch (GlobalCacheSDKException e) {
                        log.info("serverNodeConfEnv failed.");
                        UserHolder.getInstance().setSuccess(false);
                        UserHolder.getInstance().getAutopipe().add("配置globalcache服务端环境失败");
                        UserHolder.getInstance().setIsdeployfinished(true);
                        System.out.println("配置globalcache服务端环境失败");
                        e.printStackTrace();
                        return;
                    }
                    log.info("serverNodeConfEnv success.");
                    System.out.println("配置globalcache服务端环境成功");
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
                                System.out.println(entry.getKey() + ": " + line);
                                UserHolder.getInstance().getAutopipe().add(entry.getKey() + ": " + line);
                            } catch (AsyncThreadException e) {
                                System.err.println("异步线程异常");
                            }
                        }
                    }
                }

                log.info("clientNodeConfEnv start.");
                // 配置GlobalCache客户端环境
                {
                    Map<String, AsyncEntity> entityMap = new HashMap<>(hosts.size());
                    try {
                        for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.clientNodeConfEnv().entrySet()) {
                            if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                            } else {
                                log.info("clientNodeConfEnv failed.");
                                UserHolder.getInstance().setSuccess(false);
                                UserHolder.getInstance().getAutopipe().add("配置globalcache客户端环境失败");
                                UserHolder.getInstance().setIsdeployfinished(true);
                                System.out.println("配置globalcache客户端环境失败");
                                return;

                            }
                        }
                    } catch (GlobalCacheSDKException e) {
                        log.info("clientNodeConfEnv failed.");
                        e.printStackTrace();
                        UserHolder.getInstance().setSuccess(false);
                        UserHolder.getInstance().getAutopipe().add("配置globalcache客户端环境失败");
                        UserHolder.getInstance().setIsdeployfinished(true);
                        System.out.println("配置globalcache客户端环境失败");
                        return;

                    }
                    log.info("clientNodeConfEnv success.");

                    System.out.println("配置globalcache客户端环境成功");
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
                                System.out.println(entry.getKey() + ": " + line);
                                UserHolder.getInstance().getAutopipe().add(entry.getKey() + ": " + line);
                            } catch (AsyncThreadException e) {
                                System.err.println("异步线程异常");
                            }
                        }
                    }
                }

                log.info("serverNodeInstallPkgs start.");
                // 安装GlobalCache服务端
                {
                    Map<String, AsyncEntity> entityMap = new HashMap<>(hosts.size());
                    try {
                        for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.serverNodeInstallPkgs().entrySet()) {
                            if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                            } else {
                                log.info("serverNodeInstallPkgs failed.");
                                UserHolder.getInstance().setSuccess(false);
                                UserHolder.getInstance().getAutopipe().add("安装globalcache服务端环境失败");
                                UserHolder.getInstance().setIsdeployfinished(true);
                                System.out.println("安装globalcache服务端环境失败");
                                return;
                            }
                        }
                    } catch (GlobalCacheSDKException e) {
                        log.info("serverNodeInstallPkgs failed.");
                        UserHolder.getInstance().setSuccess(false);
                        e.printStackTrace();
                        UserHolder.getInstance().getAutopipe().add("安装globalcache服务端环境失败");
                        UserHolder.getInstance().setIsdeployfinished(true);
                        System.out.println("安装globalcache服务端环境失败");
                        return;
                    }
                    log.info("serverNodeInstallPkgs success.");
                    System.out.println("安装globalcache服务端环境成功");
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
                                System.out.println(entry.getKey() + ": " + line);
                                UserHolder.getInstance().getAutopipe().add(entry.getKey() + ": " + line);
                            } catch (AsyncThreadException e) {
                                System.err.println("异步线程异常");
                            }
                        }
                    }
                }

                log.info("clientNodeInstallPkgs start.");
                // 安装GlobalCache客户端
                {
                    Map<String, AsyncEntity> entityMap = new HashMap<>(hosts.size());
                    try {
                        for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.clientNodeInstallPkgs().entrySet()) {
                            if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                            } else {
                                log.info("clientNodeInstallPkgs failed.");
                                UserHolder.getInstance().getAutopipe().add("安装globalcache客户端环境失败");
                                UserHolder.getInstance().setIsdeployfinished(true);
                                System.out.println("安装globalcache客户端环境失败");
                                UserHolder.getInstance().setSuccess(false);
                                return;
                            }
                        }
                    } catch (GlobalCacheSDKException e) {
                        e.printStackTrace();
                        log.info("clientNodeInstallPkgs failed.");
                        UserHolder.getInstance().getAutopipe().add("安装globalcache客户端环境失败");
                        UserHolder.getInstance().setIsdeployfinished(true);
                        System.out.println("安装globalcache客户端环境失败");
                        UserHolder.getInstance().setSuccess(false);
                        return;
                    }
                    log.info("clientNodeInstallPkgs success.");

                    System.out.println("安装globalcache客户端环境成功");
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
                                System.out.println(entry.getKey() + ": " + line);
                                UserHolder.getInstance().getAutopipe().add(entry.getKey() + ": " + line);
                            } catch (AsyncThreadException e) {
                                System.err.println("异步线程异常");
                            }
                        }
                    }
                }

                log.info("gcServiceControl start.");
                // 首次启动GlobalCache服务
                {
                    Map<String, AsyncEntity> entityMap = new HashMap<>(hosts.size());
                    try {
                        for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.gcServiceControl(cephips, "start").entrySet()) {
                            if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                                entityMap.put(entry.getKey(), (AsyncEntity) entry.getValue().getData());
                            } else {
                                log.info("gcServiceControl failed.");
                                UserHolder.getInstance().getAutopipe().add("初始化globalcache失败");
                                UserHolder.getInstance().setIsdeployfinished(true);
                                System.out.println("初始化globalcache失败");
                                UserHolder.getInstance().setSuccess(false);
                                return;
                            }
                        }
                    } catch (GlobalCacheSDKException e) {
                        e.printStackTrace();
                        log.info("gcServiceControl failed.");
                        UserHolder.getInstance().getAutopipe().add("初始化globalcache失败");
                        UserHolder.getInstance().setIsdeployfinished(true);
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


                System.out.println("globalcache服务启动成功");
                UserHolder.getInstance().getAutopipe().add("安装成功");
                UserHolder.getInstance().setIsdeployfinished(true);
                UserHolder.getInstance().setSuccess(true);
            }
        };
        Thread thread = new Thread(myRunnable);
        thread.start();
        HashMap<String, Object> returnmap = new HashMap<>();
        returnmap.put("isBegin", true);

        return new ResponseResult<Map<String, Object>>(returnmap);
    }
}
