package com.hw.hwbackend.dao;


import com.hw.hwbackend.entity.Memory;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface MemoryRepository extends MongoRepository<Memory,String >{

//    Page<Memory> findByMemoryId(String memoryId,Pageable pageable);
}
