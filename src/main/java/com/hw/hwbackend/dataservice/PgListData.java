package com.hw.hwbackend.dataservice;

import com.hw.hwbackend.dao.PgListRepository;
import com.hw.hwbackend.entity.Pg;
import com.hw.hwbackend.entity.PgList;
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
//控制数据库中的Pg数据
@Service
public class PgListData {

    @Autowired
    private PgListRepository pgListRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    //保存
    public void savePgList(PgList pgList) {
        //如果需要自定义主键，可以在这里指定主键；如果不指定主键，MongoDB会自动生成主键
        //设置一些默认初始值。。。
        //调用dao
        //将时间戳作为数据库的主键
        pgListRepository.save(pgList);
    }
    //删除
    public void deletePgList(long time) {
        Query query = new Query(Criteria.where("time").lte(time));
        List<PgList> pgLists = mongoTemplate.find(query, PgList.class, "PgList");
        for (PgList pgList : pgLists) {
            pgListRepository.delete(pgList);
        }
    }

    //查询全部的pg信息
    public List<PgList> findpgList() {
        // 获取当前连接的节点
        UserHolder userHolder = UserHolder.getInstance();
        ArrayList<String> hosts = userHolder.getIprelation().getIps();
        Map<String,Integer> ipmap = userHolder.getIprelation().getIpMap();
        List<PgList> pgLists = new ArrayList<>();
        //查询每个节点的pg信息 合并成一个pgList
        long id = 0;
        ArrayList pgs = new ArrayList<>();
        for (int i = 0; i < hosts.size(); i++) {
            List<PgList> pgList = findpgNodeList(ipmap.get(hosts.get(i)));
            id = pgList.get(0).getId();
            for (int j = 0; j < pgList.get(0).getPgArrayList().size(); j++) {
                pgs.add(pgList.get(0).getPgArrayList().get(j));
            }
        }
        PgList pgList = new PgList();
        pgList.setPgArrayList(pgs);
        pgList.setTime(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        pgList.setId(id);
        pgLists.add(pgList);
        return pgLists;
    }
    //根据nodeId查询
    public List<PgList> findpgNodeList(int nodeId) {
        // 1100ms ~ 1500ms
        long time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();

        String id = nodeId + "1" + time;
        Long uid = Long.parseLong(id);
        Query query = new Query(Criteria.where("id").gte(uid - 1000 * 60 * 60 * 5).lt(uid + 1000 * 60 * 60 * 5))
                .limit(1).with(Sort.by("id").descending());
        List<PgList> pgLists = mongoTemplate.find(query, PgList.class, "PgList");
        return pgLists;
    }
}
