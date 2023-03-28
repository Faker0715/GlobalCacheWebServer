package com.hw.hwbackend.deleteservice;

import com.hw.hwbackend.dataservice.*;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.plus.executor.annotation.XxlRegister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

//定时删除数据库中的数据
@Service
public class AbstractDeleteService {
    @Autowired
    private CpuCalenderData cpuCalenderData;
    @Autowired
    private CpuData cpuData;
    @Autowired
    private DiskData diskData;
    @Autowired
    private HealthListData healthListData;
    @Autowired
    private MemoryData memoryData;
    @Autowired
    private NetworkData networkData;
    @Autowired
    private PtListData ptListData;
    @Autowired
    private PgListData pgListData;
    @Autowired
    private IprelationData iprelationData;

    @XxlJob("deleteData")
    @XxlRegister(cron = "0 0 0 * * ?",author = "Faker",jobDesc = "deleteData",triggerStatus = 1)
    public void delete(){
        //设置删除的时间范围
        long nowtime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        long time = nowtime - 60*60*24*1000L;
        cpuData.deleteCPU(time);
        cpuCalenderData.deleteCpuCalender(time);
        memoryData.deleteMemory(time);
        diskData.deleteDisk(time);
        networkData.deleteNetwork(time);
        time = nowtime - 60*60*24*1000L;
        healthListData.deleteHealehList(time);
        ptListData.deletePtList(time);
        pgListData.deletePgList(time);
        iprelationData.deleteIprelation(time);
    }


}
