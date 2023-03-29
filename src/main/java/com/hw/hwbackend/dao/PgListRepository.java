package com.hw.hwbackend.dao;

import com.hw.hwbackend.entity.Pg;
import com.hw.hwbackend.entity.PgList;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface PgListRepository extends MongoRepository<PgList,String > {

//    Page<CPU> findByCpuId(String Id,Pageable pageable);




}