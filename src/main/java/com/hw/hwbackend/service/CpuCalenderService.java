package com.hw.hwbackend.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hw.hwbackend.dataservice.CpuCalenderData;
import com.hw.hwbackend.dto.WebsocketDTO;
import com.hw.hwbackend.entity.CpuCalender;
import com.hw.hwbackend.entity.User;
import com.hw.hwbackend.security.WebSocketHandlerListenterImpl;
import com.hw.hwbackend.util.UserHolder;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hw.hwbackend.util.RedisConstants.CACHE_CpuCalender_KEY;
import static com.hw.hwbackend.util.RedisConstants.CACHE_CpuCalender_TIMEOUT;
//节点日历图的业务类
@Service
public class CpuCalenderService{
    @Autowired
    private CpuCalenderData cpuCalenderData;
    private static Logger log = LoggerFactory.getLogger(CpuCalenderService.class);
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    public void sendMsg(){
        //将数据封装成json
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("url","getCpuCalender");
        CpuCalender cpuCalender = getCpuCalender();
        if(BeanUtil.isNotEmpty(cpuCalender))
            jsonObject.put("params", JSONObject.toJSON(cpuCalender.getCpuNodeArrayList()));
        else
            jsonObject.put("params","");
        WebSocketHandlerListenterImpl webSocketHandlerListenter = WebSocketHandlerListenterImpl.getInstance();


        System.out.println("cpucanlender: " + webSocketHandlerListenter);
        System.out.println("cpucanlender: " + webSocketHandlerListenter.chanelIdMap);
        //根据websoket连接 判断
        for (Map.Entry<String, Channel> entry : webSocketHandlerListenter.chanelIdMap.entrySet()) {
            WebsocketDTO wsdto = webSocketHandlerListenter.WebsocketMap.get(entry.getKey());
            if(wsdto.getUrl().equals("/getCpuCalender")) {
                jsonObject.put("token",wsdto.getParams().getToken());
                log.info("cpucalenderservice-websocket: " + jsonObject.toJSONString());
                //发送给前端
                entry.getValue().writeAndFlush(new TextWebSocketFrame(jsonObject.toJSONString()));
            }
        }

    }
    public CpuCalender getCpuCalender() {
        //创建redis key
        String key = CACHE_CpuCalender_KEY;
        String json = stringRedisTemplate.opsForValue().get(key);
        List<CpuCalender> cpuCalenders = new ArrayList<>();
        //如果redis有数据直接从中取
        if(StrUtil.isNotBlank(json)){
            cpuCalenders = JSON.parseArray(json,CpuCalender.class);
        }else{
            //没有就从数据库查询
            cpuCalenders = cpuCalenderData.findCpuCalenderList();
            if(BeanUtil.isNotEmpty(cpuCalenders))
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(cpuCalenders),CACHE_CpuCalender_TIMEOUT, TimeUnit.SECONDS);
        }
        if(cpuCalenders.size() == 0){
            return null;
        }
        CpuCalender cpuCalender = cpuCalenders.get(cpuCalenders.size() - 1);
        ArrayList<CpuCalender.CpuNode> cpuNodeArrayList = cpuCalender.getCpuNodeArrayList();
        //根据nodeid排序
        for (int i = 0; i < cpuNodeArrayList.size() - 1; i++) {
            for (int j = 0; j < cpuNodeArrayList.size() - 1 - i; j++) {
                if (cpuNodeArrayList.get(j).getNodeId() > cpuNodeArrayList.get(j + 1).getNodeId()){
                    CpuCalender.CpuNode temp;
                    temp = cpuNodeArrayList.get(j);
                    cpuNodeArrayList.set(j, cpuNodeArrayList.get(j+1));
                    cpuNodeArrayList.set(j+1,temp);
                }
            }
        }
        cpuCalender.setCpuNodeArrayList(cpuNodeArrayList);
        return cpuCalender;
    }
}