package com.hw.hwbackend.dao;

import com.hw.hwbackend.entity.Cpu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface CPURepository extends MongoRepository<Cpu,String > {

    Page<Cpu> findById(String Id, Pageable pageable);

}