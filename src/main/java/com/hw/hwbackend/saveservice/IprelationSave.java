package com.hw.hwbackend.saveservice;

import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.StatusCode;
import com.hw.globalcachesdk.entity.NodeStatusInfo;
import com.hw.globalcachesdk.entity.StaticNetInfo;
import com.hw.globalcachesdk.exception.GlobalCacheSDKException;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import com.hw.globalcachesdk.executor.RegisterExecutor;
import com.hw.hwbackend.dataservice.IprelationData;
import com.hw.hwbackend.entity.Iprelation;
import com.hw.hwbackend.mapper.RegMapper;
import com.hw.hwbackend.util.UserHolder;
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
    public void IprelationSchedule() {
        Iprelation ip = new Iprelation();
        Map<Integer, String> idmap = new HashMap<>();

        HashMap<Integer, ArrayList<Integer>> disks = new HashMap<>();
        ArrayList<Integer> nodes = new ArrayList<>();
        ArrayList<String> ips = new ArrayList<>();
        Map<String, StaticNetInfo> staticNetInfomap = new HashMap<>();

        try {
            NodeStatusInfo nodeStatusInfo = (NodeStatusInfo) GlobalCacheSDK.queryNodeStatusInfoLocal();
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
        } catch (GlobalCacheSDKException e) {
            System.out.println("接口调用失败");
            e.printStackTrace();
        }
        for (String host : ips) {
            try {
                for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryStaticNetInfo(host).entrySet()) {
                    if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                        staticNetInfomap.put(host, (StaticNetInfo) entry.getValue().getData());
                    }
                }
            } catch (GlobalCacheSDKException e) {
                System.out.println("接口调用失败");
                e.printStackTrace();
            }
        }


        ip.setId(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        ip.setNodes(nodes);
        ip.setIdMap(idmap);
        ip.setDisks(disks);
        ip.setIps(ips);
        ip.setStaticNetInfomap(staticNetInfomap);
        UserHolder userHolder = UserHolder.getInstance();
        userHolder.setIprelation(ip);
        iprelationData.saveIprelation(ip);

    }
}


