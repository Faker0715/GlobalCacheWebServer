package com.hw.hwbackend.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hw.hwbackend.dataservice.MemoryData;
import com.hw.hwbackend.dto.WebsocketDTO;
import com.hw.hwbackend.entity.Memory;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hw.hwbackend.util.RedisConstants.CACHE_Memory_KEY;
import static com.hw.hwbackend.util.RedisConstants.CACHE_Memory_TIMEOUT;
//Memory信息的业务类
@Service
public class MemoryService {

    private static Logger log = LoggerFactory.getLogger(MemoryService.class);
    @Autowired
    private MemoryData memoryData;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void sendMsg(){
        //将数据封装成json
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("url","getMemoryData");
        WebSocketHandlerListenterImpl webSocketHandlerListenter = WebSocketHandlerListenterImpl.getInstance();
        //根据websoket连接 判断
        for (Map.Entry<String, Channel> entry : webSocketHandlerListenter.chanelIdMap.entrySet()) {
            WebsocketDTO wsdto = webSocketHandlerListenter.WebsocketMap.get(entry.getKey());
            if(wsdto.getUrl().equals("/getMemoryData") && wsdto.getParams().getNodeId() != -1){
                jsonObject.put("params", JSONObject.toJSON(getMemoryData(wsdto.getParams().getNodeId())));
                jsonObject.put("token",wsdto.getParams().getToken());
                //发送给前端
                entry.getValue().writeAndFlush(new TextWebSocketFrame(jsonObject.toJSONString()));
            }
        }

    }
    public Memory getMemoryData(int nodeId) {
        //创建redis key
        String key = CACHE_Memory_KEY + nodeId;
        String json = stringRedisTemplate.opsForValue().get(key);
        List<Memory> memories = new ArrayList<>();
        //如果redis有数据直接从中取
        if(StrUtil.isNotBlank(json)){
            memories = JSON.parseArray(json,Memory.class);

        }else{//没有就从数据库查询
            memories = memoryData.findMemoryListBynodeId(nodeId);
            if(BeanUtil.isNotEmpty(memories))
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(memories),CACHE_Memory_TIMEOUT,TimeUnit.SECONDS);
        }
        if(memories.size() == 0){
            return null;
        }
        //将数据封装成前端所需的格式
        Memory memory = memories.get(0);
//        System.out.println(memories.size());
        String[]  memoryNowTime = new String[memories.size()];
        Double[] memoryRatioList = new Double[memories.size()];

        for (int i = 0; i < memories.size(); i++) {
            memoryRatioList[memories.size() - i - 1] = memories.get(i).getMemoryRatio();
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");//设置日期格式
            String date = df.format(memories.get(i).getTime());
            memoryNowTime[memories.size() - i -1] = date;

        }
        memory.setMemoryRatioList(memoryRatioList);
        memory.setMemoryNowTime(memoryNowTime);
        return memory;
    }

}
