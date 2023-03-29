package com.hw.hwbackend.dataservice;

import com.hw.hwbackend.dao.MemoryRepository;
import com.hw.hwbackend.entity.Cpu;
import com.hw.hwbackend.entity.Memory;
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
//控制数据库中的Memory数据
@Service
public class MemoryData {

    @Autowired
    private MemoryRepository memoryRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 保存
     */
    public void saveMemory(Memory memory){
        //如果需要自定义主键，可以在这里指定主键；如果不指定主键，MongoDB会自动生成主键
        //设置一些默认初始值。。。
        //调用dao
        memory.setTime(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        memoryRepository.save(memory);
    }
    //删除
    public void deleteMemory(long time){
        Query query = new Query(Criteria.where("time").lte(time));
        List<Memory> memories= mongoTemplate.find(query, Memory.class, "Memory");
        for (Memory memory:memories){
            memoryRepository.delete(memory);
        }
    }
    //根据NodeId查询 最新的20条
    public List<Memory> findMemoryListBynodeId(int nodeId) {
        long time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();

        String id = nodeId + "1" + time;
        Long uid = Long.parseLong(id);
        Query query = new Query(Criteria.where("id").gte(uid - 1000*60*3).lt(uid + 1000*60*3))
                .limit(20).with(Sort.by("id").descending());
        List<Memory> memories = mongoTemplate.find(query, Memory.class, "Memory");
        return memories;
    }

}
