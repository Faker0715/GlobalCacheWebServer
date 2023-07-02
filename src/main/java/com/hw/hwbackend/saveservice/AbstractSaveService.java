package com.hw.hwbackend.saveservice;

import com.hw.hwbackend.dataservice.CpuCalenderData;
import com.hw.hwbackend.dataservice.HealthListData;
import com.hw.hwbackend.entity.Memory;
import com.hw.hwbackend.util.UserHolder;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.plus.executor.annotation.XxlRegister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
//抽象类 集中控制各项数据从集群获取数据 存放数据库
@Service
public class AbstractSaveService {
    @Autowired
    private CpuCalenderSave cpuCalenderSave;
    @Autowired
    private CpuSave cpuSave;
    @Autowired
    private DiskSave diskSave;
    @Autowired
    private HealthListSave healthListSave;
    @Autowired
    private MemorySave memorySave;
    @Autowired
    private NetworkSave networkSave;
    @Autowired
    private PgListSave pgListSave;
    @Autowired
    private PtListSave ptListSave;
    @Autowired
    private IprelationSave iprelationSave;

    @XxlJob("saveIprelation")
    @XxlRegister(cron = "0 0 0/1 * * ?",author = "Faker",jobDesc = "saveIprelation",triggerStatus = 1)
    void saveIprelation() {
        if(UserHolder.getInstance().isSuccess() == false || UserHolder.getInstance().getCeph1().equals("")){

            return;
        }
        iprelationSave.IprelationSchedule();
    }

    @XxlJob("saveCpu")
    @XxlRegister(cron = "0/5 * * * * ?",author = "Faker",jobDesc = "saveCpu",triggerStatus = 1)
    void saveCpu() {
        if(UserHolder.getInstance().isSuccess() == false || UserHolder.getInstance().getCeph1().equals("")){
            return;
        }
        cpuSave.CpuSchedule();
    }

    @XxlJob("saveDisk")
    @XxlRegister(cron = "0/5 * * * * ?",author = "Faker",jobDesc = "saveDisk",triggerStatus = 1)
    void saveDisk() {
        if(UserHolder.getInstance().isSuccess() == false || UserHolder.getInstance().getCeph1().equals("")){
            return;
        }
        diskSave.DiskSchedule();
    }

    @XxlJob("saveCpuCalender")
    @XxlRegister(cron = "0/5 * * * * ?",author = "Faker",jobDesc = "saveCpuCalender",triggerStatus = 1)
    void saveCpuCalender() {
        if(UserHolder.getInstance().isSuccess() == false || UserHolder.getInstance().getCeph1().equals("")){
            return;
        }
        cpuCalenderSave.CpuCalenderSchedule();
    }

    @XxlJob("saveMemory")
    @XxlRegister(cron = "0/5 * * * * ?",author = "Faker",jobDesc = "saveMemory",triggerStatus = 1)
    void saveMemory() {
        if(UserHolder.getInstance().isSuccess() == false || UserHolder.getInstance().getCeph1().equals("")){
            return;
        }
        memorySave.MemorySchedule();
    }

    @XxlJob("saveNetwork")
    @XxlRegister(cron = "0/5 * * * * ?",author = "Faker",jobDesc = "saveNetwork",triggerStatus = 1)
    void saveNetwork() {
        if(UserHolder.getInstance().isSuccess() == false || UserHolder.getInstance().getCeph1().equals("")){
            return;
        }
        networkSave.NetworkSchedule();
    }
    @XxlJob("savePgList")
    @XxlRegister(cron = "5 0/10 * * * ?",author = "Faker",jobDesc = "savePgList",triggerStatus = 1)
    void savePglist() {
        if(UserHolder.getInstance().isSuccess() == false || UserHolder.getInstance().getCeph1().equals("")){
            return;
        }
        pgListSave.PgListSchedule();
    }

    @XxlJob("savePtList")
    @XxlRegister(cron = "10 0/10 * * * ?",author = "Faker",jobDesc = "savePtList",triggerStatus = 1)
    void savePtlist() {
        if(UserHolder.getInstance().isSuccess() == false || UserHolder.getInstance().getCeph1().equals("")){
            return;
        }
        ptListSave.PtListSchedule();
    }
    @XxlJob("saveHealthList")
    @XxlRegister(cron = "3 0/10 * * * ?",author = "Faker",jobDesc = "saveHealthList",triggerStatus = 1)
    void saveHealthList() {
        if(UserHolder.getInstance().isSuccess() == false || UserHolder.getInstance().getCeph1().equals("")){
            return;
        }
        healthListSave.HealthListSchedule();
    }

}