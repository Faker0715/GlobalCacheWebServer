package com.hw.hwbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hw.hwbackend.entity.Ceph;
import com.hw.hwbackend.entity.Menu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    List<String> selectPermsByUserId(Long userid);

}
