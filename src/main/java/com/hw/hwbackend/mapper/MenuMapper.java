package com.hw.hwbackend.mapper;

import com.hw.hwbackend.entity.Ceph;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
@Mapper
public interface MenuMapper{

    void truncateTable();

    Integer insertCephs(List<Ceph> cephList);
    List<Ceph> selectCephs();
}
