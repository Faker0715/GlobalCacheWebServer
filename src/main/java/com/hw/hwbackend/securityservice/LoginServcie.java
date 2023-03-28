package com.hw.hwbackend.securityservice;

import com.hw.hwbackend.util.ResponseResult;
import com.hw.hwbackend.entity.User;

import javax.servlet.http.HttpServletRequest;

public interface LoginServcie {
    ResponseResult login(User user);

    ResponseResult logout(HttpServletRequest request);

    ResponseResult loginToken(HttpServletRequest request);

}
