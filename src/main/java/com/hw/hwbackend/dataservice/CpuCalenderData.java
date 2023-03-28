package com.hw.hwbackend.dataservice;

import com.hw.hwbackend.dao.CpuCalenderRepository;
import com.hw.hwbackend.entity.Cpu;
import com.hw.hwbackend.entity.CpuCalender;
import com.hw.hwbackend.entity.Memory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

//控制数据库中节点日历图数据
@Service
public class CpuCalenderData {
    @Autowired
    private CpuCalenderRepository cpuCalenderRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    /**
     * 保存
     */
    //保存节点日历图信息
    public void saveCpuCalender(CpuCalender cpuCalender) {
        //如果需要自定义主键，可以在这里指定主键；如果不指定主键，MongoDB会自动生成主键
        //设置一些默认初始值。。。
        //调用dao
        cpuCalender.setTime(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        cpuCalenderRepository.save(cpuCalender);
    }
    //删除
    public void deleteCpuCalender(long time) {
        Query query = new Query(Criteria.where("time").lte(time));
        List<CpuCalender> cpuCalenders = mongoTemplate.find(query, CpuCalender.class, "CpuCalender");
        for (CpuCalender cpuCalender : cpuCalenders) {
            cpuCalenderRepository.delete(cpuCalender);
        }
    }
    //查询节点日历图信息
    public List<CpuCalender> findCpuCalenderList() {
        long time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();

        Query query = new Query(Criteria.where("time").gte(time - 1000*60*3).lt(time + 1000*60*3))
                .limit(1).with(Sort.by("time").descending());
        List<CpuCalender> cpuCalenders = mongoTemplate.find(query, CpuCalender.class, "CpuCalender");

        return cpuCalenders;
    }
}
