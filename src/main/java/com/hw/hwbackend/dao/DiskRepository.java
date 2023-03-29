package com.hw.hwbackend.dao;

import com.hw.hwbackend.entity.Disk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DiskRepository extends MongoRepository<Disk,String >{

}