package com.hw.hwbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hw.hwbackend.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
@Mapper
public interface UserMapper extends BaseMapper<User> {

    List<String> selectPermsByUserId(Long userId);
}
