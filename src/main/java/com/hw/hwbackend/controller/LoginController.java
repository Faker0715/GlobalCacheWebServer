package com.hw.hwbackend.controller;


import com.hw.hwbackend.util.ResponseResult;
import com.hw.hwbackend.entity.User;
import com.hw.hwbackend.securityservice.LoginServcie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
public class LoginController {
    //登陆相关业务
    @Autowired
    private LoginServcie loginServcie;
    //登陆
    @PostMapping("/getLoginPassword")
    public ResponseResult login(@RequestBody User user){
        //登录
        return loginServcie.login(user);

    }
    //获取token
    @PostMapping("/getLoginToken")
    public  ResponseResult getLoginToken(HttpServletRequest request){
        //登录
        return loginServcie.loginToken(request);
    }
    //退出登陆
    @RequestMapping("/getLogOut")
    public ResponseResult logout(HttpServletRequest request){

        return loginServcie.logout(request);
    }

}
