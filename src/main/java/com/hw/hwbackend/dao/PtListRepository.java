package com.hw.hwbackend.dao;

import com.hw.hwbackend.entity.PtList;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface PtListRepository extends MongoRepository<PtList,String > {

//    Page<CPU> findByCpuId(String Id,Pageable pageable);




}