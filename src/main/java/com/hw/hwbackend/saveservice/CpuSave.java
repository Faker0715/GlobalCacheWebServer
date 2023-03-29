package com.hw.hwbackend.saveservice;

import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.StatusCode;
import com.hw.globalcachesdk.entity.CpuInfo;
import com.hw.globalcachesdk.entity.UptimeInfo;
import com.hw.globalcachesdk.exception.GlobalCacheSDKException;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import com.hw.hwbackend.dataservice.CpuData;
import com.hw.hwbackend.entity.*;
import com.hw.hwbackend.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

//从集群获取Cpu数据 存入数据库
@Service
public class CpuSave {
    @Autowired
    private CpuData cpuData;

    public void CpuSchedule() {
        //获取连接当前节点信息
        UserHolder userHolder = UserHolder.getInstance();
        ArrayList<String> hosts = userHolder.getIprelation().getIps();
        Map<String,Integer> ipmap = userHolder.getIprelation().getIpMap();
        long time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();

        CpuInfo cpuInfo = new CpuInfo();
        int worktime = 0;
        //获取运行时间
        try {
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryUptime(hosts).entrySet()) {
                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                    UptimeInfo up = (UptimeInfo) (entry.getValue().getData());
                    worktime = (int) up.getUptime();
                }
            }
        } catch (
                GlobalCacheSDKException e) {
            System.out.println("接口调用失败");
            e.printStackTrace();
        }
        //获取Cpu相关数据
        try {
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryCpuInfo(hosts).entrySet()) {
                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                    cpuInfo = (CpuInfo) entry.getValue().getData();
                    //封装
                    Cpu cpu = new Cpu();
                    cpu.setCpuUse(cpuInfo.getTotalUsage());
                    cpu.setNodeId(ipmap.get(entry.getKey()));
                    cpu.setWorkingTime(new WorkTime(worktime/60 - (worktime/(24*60))*24,worktime % 60 ,worktime/(24*60)));
                    String id = ipmap.get(entry.getKey()) + "1" + time;
                    cpu.setId(Long.parseLong(id));
                    cpu.setTime(time);
                    //保存到数据库
                    cpuData.saveCPU(cpu);
                }
            }
        } catch (GlobalCacheSDKException e) {
            System.out.println("接口调用失败");
            e.printStackTrace();
        }
    }
}


