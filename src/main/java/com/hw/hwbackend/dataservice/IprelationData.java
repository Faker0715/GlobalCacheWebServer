package com.hw.hwbackend.dataservice;

import com.hw.hwbackend.dao.IprelationRepository;
import com.hw.hwbackend.entity.Iprelation;
import com.hw.hwbackend.entity.PtList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class IprelationData {

    @Autowired
    private IprelationRepository iprelationRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public void saveIprelation(Iprelation iprelation){
        iprelationRepository.save(iprelation);
    }

    public Iprelation getIprelation() {
        long time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();

        Query query = new Query(Criteria.where("id").gte(time - 1000 * 60 * 60).lt(time + 1000*60*60)).with(Sort.by("id").descending());
        Iprelation iprelation = mongoTemplate.findOne(query,Iprelation.class,"Iprelation");
        return iprelation;
    }

    public void deleteIprelation(long time) {
        Query query = new Query(Criteria.where("time").lte(time));
        List<Iprelation> iprelations = mongoTemplate.find(query, Iprelation.class, "Iprelation");
        for (Iprelation iprelation: iprelations) {
            iprelationRepository.delete(iprelation);
        }
    }
}
