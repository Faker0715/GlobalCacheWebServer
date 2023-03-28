package com.hw.hwbackend.dataservice;

import com.hw.hwbackend.dao.CPURepository;
import com.hw.hwbackend.entity.Cpu;
import com.hw.hwbackend.entity.Disk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

//控制数据库中Cpu数据
@Service
public class CpuData {

    @Autowired
    private CPURepository cpuRepository;

    @Autowired
    private MongoTemplate mongoTemplate;



    /**
     * 保存
     */
    //====================================
    public void saveCPU(Cpu cpu){
        //如果需要自定义主键，可以在这里指定主键；如果不指定主键，MongoDB会自动生成主键
        //设置一些默认初始值。。。
        //调用dao


        cpuRepository.save(cpu);
    }
    //====================================

    public void deleteCPU(long time){
        Query query = new Query(Criteria.where("time").lte(time));
        List<Cpu> cpus = mongoTemplate.find(query, Cpu.class, "Cpu");
        for (Cpu cpu:cpus){
            cpuRepository.delete(cpu);
        }
    }




    /**
     * 根据id查询
     * @param id
     * @return
     */
    public Cpu findCPUById(String id){
        //调用dao
        return cpuRepository.findById(id).get();
    }


    public Page<Cpu> findCPUListById(String Id, int page, int size) {
        return cpuRepository.findById(Id, PageRequest.of(page-1,size));
    }

    //根据nodeId查询
    public List<Cpu> findCpubyNodeId(int nodeId) {
        long time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        String id = nodeId + "1" + time;
        Long uid = Long.parseLong(id);
        //查询最新的20条
        Query query = new Query(Criteria.where("id").gte(uid - 1000*60*3).lt(uid + 1000*60*3))
                .limit(20).with(Sort.by("id").descending());
        List<Cpu> cpus = mongoTemplate.find(query, Cpu.class, "Cpu");
        long endTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        return cpus;
    }

}
