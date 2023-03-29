package com.hw.hwbackend.service;

import com.hw.hwbackend.dataservice.HealthListData;
import com.hw.hwbackend.entity.HealthList;
import com.hw.hwbackend.saveservice.HealthListSave;
import com.hw.hwbackend.util.ResponseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//有问题的节点数量的业务类
@Service
public class HealthInfoService {
    private static Logger log = LoggerFactory.getLogger(HealthInfoService.class);
    @Autowired
    private HealthListData healthListData;
    @Autowired
    private HealthListSave healthListSave;

    public synchronized   ResponseResult getHealthInfo() {
        List<HealthList> healthLists  = healthListData.getAbnInfo();
        if(healthLists.size() == 0){
            healthListSave.HealthListSchedule();
            healthListData.getAbnInfo();
        }

        Map map = new HashMap<>();
        if (healthLists.size() != 0) {
            map.put("healthInfo", healthLists.get(healthLists.size() - 1).getHealthArrayList());
            map.put("clusterState", healthLists.get(healthLists.size() - 1).getClusterState());
        } else {
            map.put("healthInfo", "");
            map.put("clusterState", true);
        }
        return new ResponseResult<Map<String, Object>>(map);
    }

    public synchronized  ResponseResult getHealthInfoNum() {
        List<HealthList> healthLists  = healthListData.getAbnInfo();
        if(healthLists.size() == 0){
            healthListSave.HealthListSchedule();
            healthListData.getAbnInfo();
        }
        Map map = new HashMap<>();
        if (healthLists.size() != 0)
            map.put("healthInfoNum", healthLists.get(healthLists.size() - 1).getHealthArrayList().size());
        else
            map.put("healthInfoNum", 0);
        return new ResponseResult<Map<String, Object>>(map);
    }
}
