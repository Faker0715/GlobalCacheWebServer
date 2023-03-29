package com.hw.hwbackend.securityservice;

import com.hw.hwbackend.util.ResponseResult;
import com.hw.hwbackend.entity.User;

public interface RegService {
    ResponseResult reg(User user);
    ResponseResult updatePassword(String userName,String oldPassword,String newPassword);
}
