package com.hw.hwbackend.dataservice;

import com.hw.hwbackend.dao.NetworkRepository;
import com.hw.hwbackend.entity.Cpu;
import com.hw.hwbackend.entity.CpuCalender;
import com.hw.hwbackend.entity.Network;
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
//控制数据库中的Network数据
@Service
public class NetworkData {


    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 保存
     */
    public void saveNetwork(Network network) {
        //如果需要自定义主键，可以在这里指定主键；如果不指定主键，MongoDB会自动生成主键
        //设置一些默认初始值。。。
        //调用dao
        networkRepository.save(network);
    }
    //删除
    public void deleteNetwork(long time){
        Query query = new Query(Criteria.where("time").lte(time));
        List<Network> networks= mongoTemplate.find(query, Network.class, "Network");
        for (Network network:networks){
            networkRepository.delete(network);
        }
    }

    public List<Network> findNetworkList() {
        //调用dao
        return networkRepository.findAll();
    }

    //根据NodeId查询 最新的20条
    public List<Network> findNetbyNodeId(int nodeId) {
        long time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();

        String id = nodeId + "1" + time;
        Long uid = Long.parseLong(id);
        Query query = new Query(Criteria.where("id").gte(uid - 1000*60*3).lt(uid + 1000*60*3))
                .limit(20).with(Sort.by("id").descending());
        List<Network> networks = mongoTemplate.find(query, Network.class, "Network");
        return networks;
    }

}
