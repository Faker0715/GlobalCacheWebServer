package com.hw.hwbackend.entity;

import cn.hutool.core.lang.hash.Hash;
import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Data
@Document(collection = "Iprelation")
@CompoundIndex(def = "{'userid': 1, 'nickname': -1}")
public class Iprelation {
    private long id;
    private Map<Integer,String> idMap = new HashMap<>();
    private Map<Integer, ArrayList<Integer>> disks = new HashMap<>();
    private ArrayList<Integer> nodes = new ArrayList<>();
    private ArrayList<String> ips = new ArrayList<>();
    public Map<String,Integer> getIpMap(){
        Map<String,Integer> ipMap = new HashMap<>();
        for(Map.Entry<Integer,String> entry : idMap.entrySet()){
            String key = entry.getValue();
            Integer value = entry.getKey();
            ipMap.put(key,value);
        }
        return ipMap;
    }
}

