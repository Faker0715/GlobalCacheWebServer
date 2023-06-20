package com.hw.hwbackend.service;

//Cpu信息的业务类

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hw.hwbackend.dataservice.CpuData;
import com.hw.hwbackend.dto.WebsocketDTO;
import com.hw.hwbackend.entity.Cpu;
import com.hw.hwbackend.security.WebSocketHandlerListenterImpl;
import com.hw.hwbackend.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.hw.hwbackend.util.RedisConstants.CACHE_Cpu_KEY;
import static com.hw.hwbackend.util.RedisConstants.CACHE_Cpu_TIMEOUT;

@Service
public class CpuService {


    @Autowired
    private CpuData cpuData;
    private static Logger log = LoggerFactory.getLogger(CpuService.class);
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void sendMsg() {
        //将数据封装成json
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("url", "getCpuData");
        WebSocketHandlerListenterImpl webSocketHandlerListenter = WebSocketHandlerListenterImpl.getInstance();
        //根据websoket连接 判断
        for (Map.Entry<String, Channel> entry : webSocketHandlerListenter.chanelIdMap.entrySet()) {
            WebsocketDTO wsdto = webSocketHandlerListenter.WebsocketMap.get(entry.getKey());
            if(wsdto == null){
                continue;
            }
            System.out.println("key: " + entry.getKey() + " " + webSocketHandlerListenter.WebsocketMap.get(entry.getKey()));
            System.out.println(wsdto.getUrl());
            System.out.println(wsdto.getParams());
            if (wsdto.getUrl().equals("/getCpuData") && wsdto.getParams().getNodeId() != -1) {
                System.out.println("cpu will send: " + jsonObject);
                jsonObject.put("params", getCpuDataByNodeId(wsdto.getParams().getNodeId()));
                jsonObject.put("token", wsdto.getParams().getToken());
                //发送给前端
                entry.getValue().writeAndFlush(new TextWebSocketFrame(jsonObject.toJSONString()));
            }
        }
    }

    public Cpu getCpuDataByNodeId(int nodeId) {
        System.out.println("test cpu");
        //创建redis key
        String key = CACHE_Cpu_KEY + nodeId;
        String json = stringRedisTemplate.opsForValue().get(key);
        List<Cpu> cpus = new ArrayList<>();
        //如果redis有数据直接从中取
        if (StrUtil.isNotBlank(json)) {
            cpus = JSON.parseArray(json, Cpu.class);
        } else {
            //没有就从数据库查询
            cpus = cpuData.findCpubyNodeId(nodeId);
            if (BeanUtil.isNotEmpty(cpus))
                stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(cpus), CACHE_Cpu_TIMEOUT, TimeUnit.SECONDS);
        }

        if(cpus.size() == 0)
            return null;
        Cpu.CpuRatio cpuRatio = new Cpu.CpuRatio();

        String[] cpuTime = new String[cpus.size()];
        Double[] cpuUse = new Double[cpus.size()];

        //将数据封装成前端所需的格式

        for (int i = 0; i < cpus.size(); i++) {
            cpuUse[i] = cpus.get(i).getCpuUse();
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");//设置日期格式
            String date = df.format(cpus.get(i).getTime());
            cpuTime[i] = date;
        }
        cpuRatio.setCpuUse(cpuUse);
        cpuRatio.setCpuTime(cpuTime);


        Cpu cpu = cpus.get(0);
        cpu.setCpuRatio(cpuRatio);
        System.out.println("cpu : " + cpu);
        return cpu;
    }

}
