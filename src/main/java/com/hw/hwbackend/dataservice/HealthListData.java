package com.hw.hwbackend.dataservice;

import com.hw.hwbackend.dao.HealthListRepository;
import com.hw.hwbackend.entity.CpuCalender;
import com.hw.hwbackend.entity.HealthList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
//控制数据库中节点健康数据
@Service
public class HealthListData {

    @Autowired
    private HealthListRepository healthListRepository;

    @Autowired
    private MongoTemplate mongoTemplate;
    //保存
    public void saveHealthList(HealthList healthList){
        //如果需要自定义主键，可以在这里指定主键；如果不指定主键，MongoDB会自动生成主键
        //设置一些默认初始值。。。
        //调用dao
        healthList.setTime(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        healthListRepository.save(healthList);
    }
    //删除
    public void deleteHealehList(long time){
        Query query = new Query(Criteria.where("time").lte(time));
        List<HealthList> healthLists= mongoTemplate.find(query, HealthList.class, "HealthList");
        for (HealthList healthList:healthLists){
            healthListRepository.delete(healthList);
        }
    }
    //查询
    public List<HealthList> getAbnInfo() {
        long time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        Query query = new Query(Criteria.where("time").gte(time - 1000 * 60 * 60 * 5).lt(time + 1000 * 60 * 60 * 5))
                .limit(1).with(Sort.by("time").descending());
        List<HealthList> mAlarmInfo = mongoTemplate.find(query, HealthList.class, "HealthList");
        return mAlarmInfo;
    }




}
