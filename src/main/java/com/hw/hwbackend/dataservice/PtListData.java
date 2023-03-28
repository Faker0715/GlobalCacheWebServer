package com.hw.hwbackend.dataservice;

import com.hw.hwbackend.dao.PtListRepository;
import com.hw.hwbackend.entity.*;
import com.hw.hwbackend.entity.PtList;
import com.hw.hwbackend.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
//控制数据库中的Pt数据
@Service
public class PtListData {

    @Autowired
    private PtListRepository ptListRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    //删除
    public void deletePtList(long time) {
        Query query = new Query(Criteria.where("time").lte(time));
        List<PtList> ptLists = mongoTemplate.find(query, PtList.class, "PtList");
        for (PtList ptList : ptLists) {
            ptListRepository.delete(ptList);
        }
    }

    /**
     * 保存
     */
    //====================================
    public void savePtList(PtList ptList) {
        //如果需要自定义主键，可以在这里指定主键；如果不指定主键，MongoDB会自动生成主键
        //设置一些默认初始值。。。
        //调用dao

        //将时间戳作为数据库的主键
        ptList.setTime(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        ptListRepository.save(ptList);
    }

    //查询全部的pt信息
    public List<PtList> findptList() {
        // 获取当前连接的节点
        UserHolder userHolder = UserHolder.getInstance();
        ArrayList<String> hosts = userHolder.getIprelation().getIps();
        Map<String,Integer> ipmap = userHolder.getIprelation().getIpMap();
        List<PtList> ptLists = new ArrayList<>();

        //查询每个节点的pt信息 合并成一个ptList
        long id = 0;
        ArrayList pts = new ArrayList<>();
        for (int i = 0; i < hosts.size(); i++) {
            List<PtList> ptList = findptNodeList(ipmap.get(hosts.get(i)));
            id = ptList.get(0).getId();
            for (int j = 0; j < ptList.get(0).getPtArrayList().size(); j++) {
                pts.add(ptList.get(0).getPtArrayList().get(j));
            }
        }
        PtList ptList = new PtList();
        ptList.setPtArrayList(pts);
        ptList.setTime(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        ptList.setId(id);
        ptLists.add(ptList);
        return ptLists;
    }

    //根据nodeId查询
    public List<PtList> findptNodeList(int nodeId) {
        // 1100ms ~ 1500ms
        long time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();

        String id = nodeId + "1" + time;
        Long uid = Long.parseLong(id);

        Query query = new Query(Criteria.where("id").gte(uid - 1000 * 60 * 60 * 5).lt(uid + 1000 * 60 * 60 * 5 ))
                .limit(1).with(Sort.by("id").descending());
        List<PtList> ptLists = mongoTemplate.find(query, PtList.class, "PtList");
        return ptLists;
    }

}
