package com.hw.hwbackend.dao;

import com.hw.hwbackend.entity.Iprelation;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.domain.Pageable;


public interface IprelationRepository extends MongoRepository<Iprelation, String> {
//    Page<Iprelation> findBynodeId(int nodeId, Pageable pageable);
//    Page<Iprelation> findBynodeIp(String nodeIp, Pageable pageable);
}
