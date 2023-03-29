package com.hw.hwbackend.saveservice;
import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.StatusCode;
import com.hw.globalcachesdk.entity.CpuInfo;
import com.hw.globalcachesdk.exception.GlobalCacheSDKException;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import com.hw.hwbackend.dataservice.CpuCalenderData;
import com.hw.hwbackend.entity.CpuCalender;
import com.hw.hwbackend.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;

//从集群获取节点日历图数据 存入数据库
@Service
public class CpuCalenderSave {

    @Autowired
    private CpuCalenderData cpuCalenderData;
    void CpuCalenderSchedule() {
        //获取连接当前节点信息
        UserHolder userHolder = UserHolder.getInstance();
        ArrayList<String> hosts = userHolder.getIprelation().getIps();
        CpuCalender cpuCalender = new CpuCalender();
        cpuCalender.setId(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        Map<String,Integer> ipmap = userHolder.getIprelation().getIpMap();
        ArrayList<CpuCalender.CpuNode> cpuNodeArrayList = new ArrayList<>();
        //从集群获取数据
        try {
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryCpuInfo(hosts).entrySet()) {
                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                    CpuInfo cpuInfo = (CpuInfo) entry.getValue().getData();
                    int nodeId = ipmap.get(entry.getKey());
                    //封装
                    CpuCalender.CpuNode cpunode = new CpuCalender.CpuNode();
                    cpunode.setNodeId(nodeId);
                    cpunode.setNodeValue(cpuInfo.getTotalUsage());
                    cpunode.setNodeState(nodeId != -1 ? CpuCalender.CpuNode.NodeState.NODE_STATE_RUNNING : CpuCalender.CpuNode.NodeState.NODE_STATE_INVALID);
                    cpuNodeArrayList.add(cpunode);
                }
            }
        } catch (GlobalCacheSDKException e) {
            System.out.println("接口调用失败");
            e.printStackTrace();
        }
        //保存数据库
        cpuCalender.setCpuNodeArrayList(cpuNodeArrayList);
        cpuCalenderData.saveCpuCalender(cpuCalender);
    }
}


