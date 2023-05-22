package com.hw.hwbackend.controller;


import com.hw.hwbackend.service.AutoDeployService;
import com.hw.hwbackend.util.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

//自动化部署
@RestController
public class AutoController {

    @Autowired
    private AutoDeployService autoDeployService;

    //检查密码
    @PostMapping("/getCheckRootPassword")
    public ResponseResult CheckRootPassword(@RequestBody Map data){
        String password = (String) data.get("password");
        String token = (String) data.get("token");
        return autoDeployService.getCheckRootPassword(password,token);
    }
    //添加节点
    @PostMapping("/getAddIP")
    public ResponseResult AddIP(@RequestBody Map data) {
        String ipAddress = (String)data.get("ipAddress");
        String token = (String) data.get("token");
        return autoDeployService.getAddIP(ipAddress,token);
    }
    //删除节点
    @PostMapping("/getDeleteIP")
    public ResponseResult  DeleteIP(@RequestBody Map data){
        String ipAddress = (String)data.get("ipAddress");
        String token = (String) data.get("token");
        return autoDeployService.getDeleteIP(ipAddress,token);
    }
    //获取已经添加的节点列表
    @PostMapping("/getIpList")
    public ResponseResult IpList(HttpServletRequest request){
        String token = request.getHeader("token");
        return autoDeployService.getIpList(token);
    }
    //IP设置
    @PostMapping("/getIPSet")
    public ResponseResult IPSet(@RequestBody Map data){
        return autoDeployService.getIPSet(data);
    }

    //设置磁盘分类信息
    @PostMapping("/getDiskClassification")
    public ResponseResult DiskClassification(@RequestBody Map data){
        return autoDeployService.getDiskClassification(data);
    }
    //设置集群信息
    @PostMapping("/getClusterSet")
    public ResponseResult ClusterSet(@RequestBody Map data){
        return autoDeployService.getClusterSet(data);
    }
    //返回设置好的信息
    @PostMapping("/getAffirmSet")
    public ResponseResult AffirmSet(@RequestBody Map data){
        return autoDeployService.getAffirmSet(data);
    }
    //部署
    @PostMapping("/getInstall")
    public ResponseResult Install(@RequestBody Map data){
        return autoDeployService.getInstall(data);
    }
    //返回已经设置的集群信息
    @PostMapping("/getClusterInfo")
    public ResponseResult ClusterInfo(@RequestBody Map data){
        return autoDeployService.getClusterInfo(data);
    }

    @PostMapping("/getLog")
    public ResponseResult getLog(@RequestBody Map data){
        return autoDeployService.getLog(data);
    }

}
