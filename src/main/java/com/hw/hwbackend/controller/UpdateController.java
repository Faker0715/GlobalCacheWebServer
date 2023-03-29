package com.hw.hwbackend.controller;


import com.hw.hwbackend.service.HealthInfoService;
import com.hw.hwbackend.service.UpdateService;
import com.hw.hwbackend.util.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

//Pt Pg 健康信息获取
@RestController
public class UpdateController {

    @Autowired
    private UpdateService updateServcie;
    @Autowired
    private HealthInfoService healthInfoNumService;

    //获取Pt信息
    @PostMapping("/getPtAll")
    public ResponseResult getPtUpdate(@RequestBody Map data){
        String token = (String) data.get("token");
        return updateServcie.getPtUpdate(token);

    }
    //获取Pg信息
    @PostMapping("/getPgAll")
    public ResponseResult getPgUpdate(@RequestBody Map data){
        String token = (String) data.get("token");
        return updateServcie.getPgUpdate(token);
    }
    @PostMapping("getHealthInfo")
    public ResponseResult getHealthInfo(){
        return healthInfoNumService.getHealthInfo();
    }
    @PostMapping("getHealthInfoNum")
    public ResponseResult getHealthInfoNum(){
        return healthInfoNumService.getHealthInfoNum();
    }
}
