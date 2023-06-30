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
        Map<String,Integer> ipmap = userHolder.getIprelation().getIpMap();
        ArrayList<CpuCalender.CpuNode> cpuNodeArrayList = new ArrayList<>();
        log.info("cpucalender-hosts: " + hosts.toString());
        String ceph1 = userHolder.getCeph1();

        // 获取cpu状态

        NodeStatusInfo nodeStatusInfo = new NodeStatusInfo();
        Map<String,ArrayList<NodeStatusInfo.NodeState>> statusmap = new HashMap<>();

        int tryTimes = 5;
        int oldTimeout = 0;
        try {
            // 记录当前接口默认超时等待时间
            oldTimeout = GlobalCacheSDK.getCommandTimeout(RegisterExecutor.QUERY_NODE_STATUS_INFO);
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
                            statusmap.put(node.getClusterIp(),node.getStateList());
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



        //从集群获取数据
        try {
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryCpuInfo(hosts).entrySet()) {
                log.info("cpucalender-querycpuinfo: " + entry.getValue().getStatusCode());
                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                    CpuInfo cpuInfo = (CpuInfo) entry.getValue().getData();
                    int nodeId = ipmap.get(entry.getKey());
                    //封装
                    CpuCalender.CpuNode cpunode = new CpuCalender.CpuNode();
                    cpunode.setNodeId(nodeId);
                    cpunode.setNodeValue(cpuInfo.getTotalUsage());

                    ArrayList<NodeStatusInfo.NodeState> statusarr = statusmap.get(entry.getKey());
                    CpuCalender.CpuNode.NodeState state;
                    if(statusarr.contains(NODE_STATE_RUNNING) && statusarr.contains(NODE_STATE_IN)){
                        state = CpuCalender.CpuNode.NodeState.NODE_STATE_RUNNING;
                    }else{
                        state = CpuCalender.CpuNode.NodeState.NODE_STATE_INVALID;
                    }
                    cpunode.setNodeState(state);
                    cpuNodeArrayList.add(cpunode);
                }
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


