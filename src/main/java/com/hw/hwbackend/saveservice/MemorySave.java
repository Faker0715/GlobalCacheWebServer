package com.hw.hwbackend.saveservice;

import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.StatusCode;
import com.hw.globalcachesdk.entity.CpuInfo;
import com.hw.globalcachesdk.entity.MemInfo;
import com.hw.globalcachesdk.entity.UptimeInfo;
import com.hw.globalcachesdk.exception.GlobalCacheSDKException;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import com.hw.hwbackend.dataservice.CpuData;
import com.hw.hwbackend.dataservice.MemoryData;
import com.hw.hwbackend.entity.Cpu;
import com.hw.hwbackend.entity.Memory;
import com.hw.hwbackend.entity.Time;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

//从集群获取Memory数据 存入数据库
@Service
public class MemorySave {
    @Autowired
    private MemoryData memoryData;

    public void MemorySchedule() {

        long stime = System.currentTimeMillis();
        long time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();

        //获取连接当前节点信息
        UserHolder userHolder = UserHolder.getInstance();
        ArrayList<String> hosts = userHolder.getIprelation().getIps();
        Map<String,Integer> ipmap = userHolder.getIprelation().getIpMap();
        MemInfo memInfo = new MemInfo();
        //获取Memory信息
        try {
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryMemInfo(hosts).entrySet()) {
                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                    memInfo = (MemInfo) (entry.getValue().getData());
                    //封装
                    Memory m = new Memory();
                    m.setMemoryUsing(((double)memInfo.getUsed() * 1.0) / (1024 * 1024));
                    m.setMemoryCache(((double)memInfo.getCache() * 1.0) / (1024 * 1024));
                    m.setMemoryUseable(((double)memInfo.getFree()*1.0)/ (1024 * 1024));
                    m.setMemoryRatio((double)((double)memInfo.getUsed()*1.0)*100 /( (double)memInfo.getTotal() * 1.0));
                    m.setNodeId(ipmap.get(entry.getKey()));
                    String id = ipmap.get(entry.getKey()) + "1" + time;
                    m.setId(Long.parseLong(id));
                    //保存
                    memoryData.saveMemory(m);
                }
            }
        } catch (GlobalCacheSDKException e) {
            System.out.println("接口调用失败");
            e.printStackTrace();
        }

        long etime = System.currentTimeMillis();
        // 计算执行时间
        System.out.printf("memorysave time: %d ms.", (etime - stime));

    }


}


