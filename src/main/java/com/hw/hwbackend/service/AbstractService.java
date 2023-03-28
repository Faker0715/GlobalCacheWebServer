package com.hw.hwbackend.service;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.hw.hwbackend.entity.LoginUser;
import com.hw.hwbackend.util.UserHolder;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.plus.executor.annotation.XxlRegister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

//抽象类 集中控制向前端定时发送数据
@Service
public class AbstractService {
    public AbstractService() {
    }

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CpuCalenderService cpuCalenderService;
    @Autowired
    private CpuService cpuService;
    @Autowired
    private DiskService diskService;
    @Autowired
    private HealthInfoService healthInfoNumService;
    @Autowired
    private MemoryService memoryService;
    @Autowired
    private NetworkService networkService;


    @XxlJob("deleteLoginUser")
    @XxlRegister(cron = "* * 5 * * ?",author = "Faker",jobDesc = "deleteLoginUser",triggerStatus = 1)
    public void deleteLoginUser(){
        Set<String> keys = stringRedisTemplate.keys("*");
        long time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();

        for(String string:keys){
            if(keys.contains("jwt")){
                LoginUser loginUser =  JSON.parseObject(stringRedisTemplate.opsForValue().get(string),LoginUser.class);
                if(BeanUtil.isNotEmpty(loginUser)){
                    // 如果两小时没有响应 那么就删掉这个用户缓存
                    if(time - loginUser.getTime() > 1000L * 60 * 60 * 2){
                        stringRedisTemplate.delete(string);
                    }
                }
            }
        }
    }


    @XxlJob("getCpuCalender")
    @XxlRegister(cron = "0/5 * * * * ?",author = "Faker",jobDesc = "getCpuCalender",triggerStatus = 1)
    public void getCpuCalender() {
        if(UserHolder.getInstance().isIsdeployfinished() == false){
            return;
        }
        cpuCalenderService.sendMsg();
    }

    @XxlJob("getCpuData")
    @XxlRegister(cron = "0/5 * * * * ?",author = "Faker",jobDesc = "getCpuData",triggerStatus = 1)
    public void getCpuData() {
        if(UserHolder.getInstance().isIsdeployfinished() == false){
            return;
        }
        cpuService.sendMsg();
    }
    @XxlJob("getMemoryData")
    @XxlRegister(cron = "0/5 * * * * ?",author = "Faker",jobDesc = "getMemoryData",triggerStatus = 1)
    public void getMemoryData() {
        if(UserHolder.getInstance().isIsdeployfinished() == false){
            return;
        }
        memoryService.sendMsg();
    }

    @XxlJob("getDiskData")
    @XxlRegister(cron = "0/5 * * * * ?",author = "Faker",jobDesc = "getDiskData",triggerStatus = 1)
    public void getDiskData() {
        if(UserHolder.getInstance().isIsdeployfinished() == false){
            return;
        }
        diskService.sendMsg();
    }

    @XxlJob("getNetData")
    @XxlRegister(cron = "0/5 * * * * ?",author = "Faker",jobDesc = "getNetData",triggerStatus = 1)
    public void getNetData() {
        if(UserHolder.getInstance().isIsdeployfinished() == false){
            return;
        }
        networkService.sendMsg();
    }
}