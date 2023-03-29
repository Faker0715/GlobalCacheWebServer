package com.hw.hwbackend.dataservice;

import com.hw.hwbackend.dao.DiskRepository;
import com.hw.hwbackend.entity.Cpu;
import com.hw.hwbackend.entity.Disk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
//控制数据库中Disk数据
@Service
public class DiskData {

    //***********************************************
    @Autowired
    private DiskRepository diskRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 保存
     */
    public void saveDisk(Disk disk){
        //如果需要自定义主键，可以在这里指定主键；如果不指定主键，MongoDB会自动生成主键
        //设置一些默认初始值。。。
        //调用dao
        disk.setTime(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        diskRepository.save(disk);
    }
    //删除
    public void deleteDisk(long time){

        Query query = new Query(Criteria.where("time").lt(time));
        List<Disk> disks = mongoTemplate.find(query,Disk.class,"Disk");
        for (Disk disk:disks){
            diskRepository.delete(disk);
        }

    }

    //根据NodeId查询 最新的20条
    public List<Disk> findDiskbyNodeId(int nodeId) {
        long time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        String id = nodeId + "1" + time;
        Long uid = Long.parseLong(id);
        Query query = new Query(Criteria.where("id").gte(uid - 1000*60*3).lt(uid + 1000*60*3))
                .limit(20).with(Sort.by("id").descending());
        List<Disk> disks = mongoTemplate.find(query, Disk.class, "Disk");
        return disks;
    }

}

