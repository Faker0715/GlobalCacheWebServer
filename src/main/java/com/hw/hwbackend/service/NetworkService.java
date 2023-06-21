package com.hw.hwbackend.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hw.hwbackend.dataservice.NetworkData;
import com.hw.hwbackend.dto.WebsocketDTO;
import com.hw.hwbackend.entity.Network;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.hw.hwbackend.util.RedisConstants.CACHE_Network_KEY;
import static com.hw.hwbackend.util.RedisConstants.CACHE_Network_TIMEOUT;
//Network信息的业务类
@Service
public class NetworkService {

    @Autowired
    private NetworkData networkData;
    private static Logger log = LoggerFactory.getLogger(NetworkService.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void sendMsg() {
        //将数据封装成json
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("url", "getNetData");

        WebSocketHandlerListenterImpl webSocketHandlerListenter = WebSocketHandlerListenterImpl.getInstance();
        //根据websoket连接 判断
        for (Map.Entry<String, Channel> entry : webSocketHandlerListenter.chanelIdMap.entrySet()) {
            WebsocketDTO wsdto = webSocketHandlerListenter.WebsocketMap.get(entry.getKey());
            if(wsdto == null){
                continue;
            }
            if(wsdto.getUrl().equals("/getNetData") && wsdto.getParams().getNodeId() != -1){
                jsonObject.put("params", JSONObject.toJSON(getNetData(wsdto.getParams().getNodeId())));
                jsonObject.put("token",wsdto.getParams().getToken());
                //发送给前端
                entry.getValue().writeAndFlush(new TextWebSocketFrame(jsonObject.toJSONString()));
            }
        }
    }

    private List<Network> getNetData(int nodeId) {
        //创建redis key
        String key = CACHE_Network_KEY + nodeId;
        String json = stringRedisTemplate.opsForValue().get(key);
        List<Network> networkList = new ArrayList<>();
        if(StrUtil.isNotBlank(json)){//如果redis有数据直接从中取
            networkList = JSON.parseArray(json,Network.class);
        }else{ //没有就从数据库查询
            networkList = networkData.findNetbyNodeId(nodeId);
            if(BeanUtil.isNotEmpty(networkList))
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(networkList),CACHE_Network_TIMEOUT, TimeUnit.SECONDS);
        }
        if(networkList.size() == 0){
            return null;
        }
        return networkList;
    }

}
