package com.hw.hwbackend.dao;

import com.hw.hwbackend.entity.HealthList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HealthListRepository extends MongoRepository<HealthList,String > {

//    Page<HealthList> findByHealthId(String healthId, Pageable pageable);

}
