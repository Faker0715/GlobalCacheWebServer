package com.hw.hwbackend.dao;

import com.hw.hwbackend.entity.Network;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NetworkRepository extends MongoRepository<Network,String >{
    Page<Network> findByNetId(String netId, Pageable pageable);
}
