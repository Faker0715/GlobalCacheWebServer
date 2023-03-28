package com.hw.hwbackend.controller;


import com.hw.hwbackend.dto.UserDTO;
import com.hw.hwbackend.util.ResponseResult;
import com.hw.hwbackend.entity.User;
import com.hw.hwbackend.securityservice.RegService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class RegController {

    @Autowired
    private RegService regService;
    @PostMapping("/getRegister")
    public ResponseResult register(@RequestBody User user) {
        return regService.reg(user);

    }
    @RequestMapping("/getUpdatePassword")
    public ResponseResult updatepassword(@RequestBody UserDTO userDTO){
        return regService.updatePassword(userDTO.getUserName(), userDTO.getOldPassword(), userDTO.getNewPassword());
    }
}

