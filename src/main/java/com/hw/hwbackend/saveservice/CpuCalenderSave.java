package com.hw.hwbackend.saveservice;

import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.StatusCode;
import com.hw.globalcachesdk.entity.CpuInfo;
import com.hw.globalcachesdk.entity.NodeStatusInfo;
import com.hw.globalcachesdk.exception.GlobalCacheSDKException;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import com.hw.globalcachesdk.executor.RegisterExecutor;

import com.hw.globalcachesdk.entity.NodeStatusInfo;
import com.hw.hwbackend.dataservice.CpuCalenderData;
import com.hw.hwbackend.entity.CpuCalender;
import com.hw.hwbackend.entity.User;
import com.hw.hwbackend.service.SessionService;
import com.hw.hwbackend.util.UserHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.hw.globalcachesdk.entity.NodeStatusInfo.NodeState.NODE_STATE_IN;
import static com.hw.globalcachesdk.entity.NodeStatusInfo.NodeState.NODE_STATE_RUNNING;
import static java.lang.Thread.sleep;

//从集群获取节点日历图数据 存入数据库
@Service
public class CpuCalenderSave {


    @Autowired
    private CpuCalenderData cpuCalenderData;
    private static Logger log = LoggerFactory.getLogger(CpuCalenderSave.class);

    void CpuCalenderSchedule() {
        long stime = System.currentTimeMillis();
        //获取连接当前节点信息
        UserHolder userHolder = UserHolder.getInstance();
        ArrayList<String> hosts = userHolder.getIprelation().getIps();
        CpuCalender cpuCalender = new CpuCalender();
        cpuCalender.setId(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        Map<String, Integer> ipmap = userHolder.getIprelation().getIpMap();
        ArrayList<CpuCalender.CpuNode> cpuNodeArrayList = new ArrayList<>();
        log.info("cpucalender-hosts: " + hosts.toString());
        // 获取cpu状态
        Map<String, ArrayList<NodeStatusInfo.NodeState>> statusmap = new HashMap<>();
        try {
            NodeStatusInfo nodeStatusInfo = (NodeStatusInfo) GlobalCacheSDK.queryNodeStatusInfoLocal();
            System.out.println(nodeStatusInfo.toString());
            for (NodeStatusInfo.Node node : nodeStatusInfo.getNodeList()) {
                statusmap.put(node.getClusterIp(), node.getStateList());
            }
        } catch (GlobalCacheSDKException e) {
            System.out.println("接口调用失败");
            e.printStackTrace();
        }

        //从集群获取数据
        try {
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryCpuInfo(hosts).entrySet()) {
                log.info("cpucalender-querycpuinfo: " + entry.getValue().getStatusCode());
                CpuCalender.CpuNode cpunode = new CpuCalender.CpuNode();
                cpunode.setIsOnline(UserHolder.getInstance().getClusterMap().get(entry.getKey()));
                int nodeId = ipmap.get(entry.getKey());
                cpunode.setNodeId(nodeId);
                if(cpunode.getIsOnline() == false){
                    System.out.println("cpu not online: " + entry.getKey());
                }
                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                    CpuInfo cpuInfo = (CpuInfo) entry.getValue().getData();
                    cpunode.setNodeValue(cpuInfo.getTotalUsage());
                    ArrayList<NodeStatusInfo.NodeState> statusarr = statusmap.get(entry.getKey());
                    CpuCalender.CpuNode.NodeState state;
                    if (statusarr.contains(NODE_STATE_RUNNING)){
                        cpunode.setIsRunning(CpuCalender.CpuNode.NodeState.NODE_STATE_RUNNING);
                    }else{
                        cpunode.setIsRunning(CpuCalender.CpuNode.NodeState.NODE_STATE_INVALID);
                    }
                    if (statusarr.contains(NODE_STATE_IN)) {
                        cpunode.setIsIn(CpuCalender.CpuNode.NodeState.NODE_STATE_IN);
                    }else{
                        cpunode.setIsIn(CpuCalender.CpuNode.NodeState.NODE_STATE_OUT);
                    }
                }
                cpuNodeArrayList.add(cpunode);
            }
        } catch (GlobalCacheSDKException e) {
            System.out.println("接口调用失败");
            e.printStackTrace();
        }
        log.info("cpucalender-cpuNodeArrayList: " + cpuNodeArrayList.toString());
        //保存数据库
        cpuCalender.setCpuNodeArrayList(cpuNodeArrayList);
        cpuCalenderData.saveCpuCalender(cpuCalender);

        long etime = System.currentTimeMillis();
        System.out.printf("cpucalendersave time: %d ms.", (etime - stime));
    }
}


