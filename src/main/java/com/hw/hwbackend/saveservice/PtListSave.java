package com.hw.hwbackend.saveservice;

import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.StatusCode;
import com.hw.globalcachesdk.entity.PtInfo;
import com.hw.globalcachesdk.entity.PtIoInfo;
import com.hw.globalcachesdk.exception.GlobalCacheSDKException;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import com.hw.globalcachesdk.executor.RegisterExecutor;
import com.hw.hwbackend.dataservice.PtListData;
import com.hw.hwbackend.entity.Pt;
import com.hw.hwbackend.entity.PtList;
import com.hw.hwbackend.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;


//从集群获取Pt数据 存入数据库
@Service
public class PtListSave {

    @Autowired
    private PtListData ptListData;

    public void PtListSchedule() {
        //获取连接当前节点信息
        String cephip = UserHolder.getInstance().getCeph1();
        UserHolder userHolder = UserHolder.getInstance();
        ArrayList<String> hosts = userHolder.getIprelation().getIps();
        Map<Integer, String> idmap = userHolder.getIprelation().getIdMap();
        Map<String, Integer> ipmap = userHolder.getIprelation().getIpMap();
        long time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();

        int tryTimes = 5;
        int oldTimeout = 0;
        try {
            // 记录当前接口默认超时等待时间
            oldTimeout = GlobalCacheSDK.getCommandConf(RegisterExecutor.QUERY_PT_IO_INFO).getTimeout();
        } catch (GlobalCacheSDKException e) {
            System.out.println("获取执行时间失败");
            e.printStackTrace();
        }
        // 时长增长因子
        double increaseFactor = 1.5f;
        // 当前接口超时等待时间
        int curTimeout = oldTimeout;
        boolean flag = false;

        ArrayList<PtIoInfo.Pt> ptiolist = new ArrayList<>();
        for(int i = 0;i < tryTimes;++i){
            if(flag)
                break;
            try {
                for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryPtIoInfo(cephip).entrySet()) {
                    if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                        PtIoInfo ptIoInfo = (PtIoInfo) entry.getValue().getData();
                        ptiolist = ptIoInfo.getPtArrayList();
                        flag = true;
                    } else if (entry.getValue().getStatusCode() == StatusCode.EXEC_COMMAND_FAILED) {
                        // 处理线程执行中断的情况 -> 休眠，等待下一次尝试
                        sleep(oldTimeout);
                    } else if (entry.getValue().getStatusCode() == StatusCode.EXEC_COMMAND_TIMEOUT) {
                        // 处理请求超时的情况 -> 增长至 increaseFactor * curTimeout
                        curTimeout = (int) Math.ceil(curTimeout * increaseFactor);
                        GlobalCacheSDK.setCommandTimeout(RegisterExecutor.QUERY_PT_IO_INFO, curTimeout);
                    }
                }
            } catch (GlobalCacheSDKException | InterruptedException e) {
                System.out.println("接口调用失败");
                e.printStackTrace();
            }
        }


        try {
            GlobalCacheSDK.setCommandTimeout(RegisterExecutor.QUERY_PT_IO_INFO, oldTimeout);
        } catch (GlobalCacheSDKException e) {
            System.out.println("设置执行时间失败");
            e.printStackTrace();
        }

        try {
            // 记录当前接口默认超时等待时间
            oldTimeout = GlobalCacheSDK.getCommandConf(RegisterExecutor.QUERY_ALL_PT_INFO).getTimeout();
        } catch (GlobalCacheSDKException e) {
            System.out.println("获取执行时间失败");
            e.printStackTrace();
        }
        flag = false;

        ArrayList<PtInfo.Pt> ptlist = new ArrayList<>();
        for(int i = 0;i < tryTimes;++i)
        {
            if(flag)
                break;
            try {
                for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryAllPtInfo(cephip).entrySet()) {
                    if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                        PtInfo ptInfo = (PtInfo) entry.getValue().getData();
                        ptlist = ptInfo.getPtList();
                        flag = true;
                    } else if (entry.getValue().getStatusCode() == StatusCode.EXEC_COMMAND_FAILED) {
                        // 处理线程执行中断的情况 -> 休眠，等待下一次尝试
                        sleep(oldTimeout);
                    } else if (entry.getValue().getStatusCode() == StatusCode.EXEC_COMMAND_TIMEOUT) {
                        // 处理请求超时的情况 -> 增长至 increaseFactor * curTimeout
                        curTimeout = (int) Math.ceil(curTimeout * increaseFactor);
                        GlobalCacheSDK.setCommandTimeout(RegisterExecutor.QUERY_ALL_PT_INFO, curTimeout);
                    }
                }
            } catch (GlobalCacheSDKException | InterruptedException e) {
                System.out.println("接口调用失败");
                e.printStackTrace();
            }
        }

        try {
            GlobalCacheSDK.setCommandTimeout(RegisterExecutor.QUERY_ALL_PT_INFO, oldTimeout);
        } catch (GlobalCacheSDKException e) {
            System.out.println("设置执行时间失败");
            e.printStackTrace();
        }
        //根据nodeId将数据分类 封装 存入数据库




        //根据nodeId将数据分类 封装 存入数据库
        for (int i = 0; i < hosts.size(); i++) {
            ArrayList<Pt> aptInfo = new ArrayList<>();
            for (PtInfo.Pt pt : ptlist) {
                for (PtIoInfo.Pt pttoio : ptiolist) {
                    PtIoInfo.PtIo ptio = pttoio.getIoInfo();
                    //将PtIo信息和Pt信息合并
                    if (pttoio.getPtId() == pt.getPtId()) {
                        Pt curpt = new Pt();
                        curpt.setPtId(pt.getPtId());
                        curpt.setBv(pt.getBv());
                        curpt.setIndexNode(pt.getIndexInNode());

                        Pt.IoInfo curioinfo = new Pt.IoInfo();
                        curioinfo.setReadSize(ptio.getReadSize());
                        curioinfo.setReadCount(ptio.getReadCount());
                        curioinfo.setIoCount(ptio.getIoCount());
                        curioinfo.setWriteSize(ptio.getWriteSize());
                        curioinfo.setWriteCount(ptio.getWriteSizeCount());

                        ArrayList<Integer> ptmaparray = new ArrayList<>();
                        ptmaparray.add(pt.getPtMapInfo().getNodeId());
                        ptmaparray.add(pt.getPtMapInfo().getDiskId());
                        ptmaparray.add(pt.getPtMapInfo().getVnodeId());
                        curpt.getPtInfo().add(ptmaparray);
                        ArrayList<Integer> backupptmaparray = new ArrayList<>();
                        backupptmaparray.add(pt.getBackupPtMapInfo().getNodeId());
                        backupptmaparray.add(pt.getBackupPtMapInfo().getDiskId());
                        backupptmaparray.add(pt.getBackupPtMapInfo().getVnodeId());
                        curpt.getPtInfo().add(backupptmaparray);
                        curpt.setIoInfo(curioinfo);
                        curpt.setState(pt.getState());
                        curpt.setNodeIp(idmap.get(pt.getPtMapInfo().getNodeId()));
                        curpt.setDiskId(pt.getPtMapInfo().getDiskId());
                        curpt.setNodeId(pt.getPtMapInfo().getNodeId());
                        if (curpt.getNodeId() == ipmap.get(hosts.get(i))){
                            aptInfo.add(curpt);
                        }

                    }
                }
            }
            //封装
            PtList ptList = new PtList();
            ptList.setPtArrayList(aptInfo);
            String id = ipmap.get(hosts.get(i)) + "1" + time;
            ptList.setId(Long.parseLong(id));
            //保存到数据库
            ptListData.savePtList(ptList);

        }

    }

}


