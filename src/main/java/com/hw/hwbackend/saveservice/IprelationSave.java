package com.hw.hwbackend.saveservice;

import cn.hutool.core.lang.hash.Hash;
import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.StatusCode;
import com.hw.globalcachesdk.entity.NodeStatusInfo;
import com.hw.globalcachesdk.entity.PgInfo;
import com.hw.globalcachesdk.exception.GlobalCacheSDKException;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import com.hw.globalcachesdk.executor.RegisterExecutor;
import com.hw.hwbackend.dataservice.IprelationData;
import com.hw.hwbackend.dataservice.PgListData;
import com.hw.hwbackend.entity.HealthList;
import com.hw.hwbackend.entity.Iprelation;
import com.hw.hwbackend.entity.PgList;
import com.hw.hwbackend.mapper.RegMapper;
import com.hw.hwbackend.util.UserHolder;
import com.mongodb.client.model.IndexOptionDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;


@Service
public class IprelationSave {

    @Autowired
    private IprelationData iprelationData;
    @Autowired
    private RegMapper regMapper;

    public void IprelationSchedule() {
        String cephip = regMapper.getIp();
        if(UserHolder.getInstance().getCeph1() != cephip){
            UserHolder.getInstance().setCeph1(cephip);
        }
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
                for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryNodeStatusInfo(cephip).entrySet()) {
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
        ip.setNodes(nodes);
        ip.setIdMap(idmap);
        ip.setDisks(disks);
        ip.setIps(ips);
        UserHolder userHolder = UserHolder.getInstance();
        userHolder.setIprelation(ip);
        userHolder.setIsdeployfinished(true);
        userHolder.setCeph1(cephip);
        iprelationData.saveIprelation(ip);

    }
}


