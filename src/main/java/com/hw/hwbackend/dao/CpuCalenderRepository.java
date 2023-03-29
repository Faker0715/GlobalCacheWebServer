package com.hw.hwbackend.dao;

import com.hw.hwbackend.entity.CpuCalender;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CpuCalenderRepository extends MongoRepository<CpuCalender,String > {

//        Page<CpuCalender> findByCpuCalenderyId(String cpuCalenderId, Pageable pageable);

}
