package com.hw.hwbackend.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hw.hwbackend.entity.GlobalCacheUser;
import com.hw.hwbackend.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;


@Mapper
public interface RegMapper extends BaseMapper<User> {
    @Select("select user_name from hwlogin.sys_user where user_name = #{userName}")
    public String selectUserName(String userName);

    @Select("select password from hwlogin.sys_user where user_name = #{userName}")
    public String selectUserPassword(String userName);

    @Insert("insert into hwlogin.sys_user(user_name, password,status,email,phonenumber,user_type,is_finished) values(#{userName}, #{userPassword},#{userStatus},#{userEmail},#{userPhoneNumber},#{userType},#{isFinished})")
    public void addUser(String userName, String userPassword,String userStatus,String userEmail,String userPhoneNumber,String userType,int isFinished);


    @Update("UPDATE hwlogin.finish SET is_finished = 1")
    public void setfinished();

    @Update("UPDATE hwlogin.finish SET is_finished = 0")
    public void setnofinished();

    @Select("select * from hwlogin.sys_user where user_name = #{username}")
    public User getUserbyName(String username);

    @Select("select * from hwlogin.globalcacheuser")
    public List<GlobalCacheUser> getuser();
    @Select("select is_finished from hwlogin.finish where id=0")
    public int getfinished();

    @Update("update hwlogin.sys_user set password=#{userPassword} where user_name=#{userName}")
    public void updataPassword(String userName,String userPassword);
}
