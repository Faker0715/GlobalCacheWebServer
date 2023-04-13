package com.hw.hwbackend.securityservice.impl;

import com.hw.hwbackend.entity.User;
import com.hw.hwbackend.mapper.RegMapper;
import com.hw.hwbackend.securityservice.RegService;
import com.hw.hwbackend.util.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RegServiceImpl implements RegService {
    @Autowired
    private RegMapper regMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    public ResponseResult reg(User user)
    {
        String userName = user.getUserName();
        String userPassword = user.getPassword();
        String userStatus = "0";
        String userEmail = user.getEmail();
        String userPhoneNumber = user.getPhonenumber();
        user.setIsFinished(0);
        String userType = "1";
        Map<String,Object> map = new HashMap<>();
        map.put("data",user);
        // 用户存在
        if(regMapper.selectUserName(userName) != null){
            map.put("isSuccessed",false);
            map.put("reason","用户已经存在");
            return new ResponseResult<Map<String,Object>>(map);
        }
//        System.out.println(userName + "***" + userPassword);
        String encode = passwordEncoder.encode(userPassword);
        regMapper.addUser(userName,encode,userStatus,userEmail,userPhoneNumber,userType,0);
        map.put("isSuccessed",true);
        map.put("reason","注册成功");
        return new ResponseResult<Map<String,Object>>(map);
    }

    @Override
    public ResponseResult updatePassword(String userName, String oldPassword, String newPassword) {
        Map<String,Object> returnmap = new HashMap<>();
        if(oldPassword==regMapper.selectUserPassword(userName))
        {
            returnmap.put("isUpdated",false);
            return new ResponseResult<Map<String,Object>>(returnmap);

        }
        String encode = passwordEncoder.encode(newPassword);
        regMapper.updataPassword(userName,encode);
        returnmap.put("isUpdated",true);
        return new ResponseResult<Map<String,Object>>(returnmap);
    }
}
