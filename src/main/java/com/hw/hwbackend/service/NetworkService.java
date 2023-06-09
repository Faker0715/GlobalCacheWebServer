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
            if(wsdto.getUrl().equals("/getNetData") && wsdto.getParams().getNodeId() != -1){
                jsonObject.put("params", JSONObject.toJSON(getNetData(wsdto.getParams().getNodeId())));
                jsonObject.put("token",wsdto.getParams().getToken());
                //发送给前端
                entry.getValue().writeAndFlush(new TextWebSocketFrame(jsonObject.toJSONString()));
            }
        }
    }

    private Network getNetData(int nodeId) {
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
        //将数据封装成前端所需的格式
        List<Network.NetData> netDataList = new ArrayList<>();
        List<Integer> netId = new ArrayList<>();
        //将数据合并
        for (Network network:networkList) {
            if (netId.size() == 0){
                netId.add(network.getNetId());
            }
            if (!netId.contains(network.getNetId())){
                netId.add(network.getNetId());
            }
        }
        for (int k = 0; k < netId.size(); k++) {
            List<Network> networks = new ArrayList<>();
            for (Network network : networkList){
                if (network.getNetId() == netId.get(k)){
                    networks.add(network);
                }
            }
            String[]  netNowTime = new String[networks.size()];
            Double[] netResolve = new Double[networks.size()];
            Double[] netSend = new Double[networks.size()];
            for (int i = 0; i < networks.size(); i++) {
                netSend[networks.size() -i -1] = networks.get(i).getSend();
                netResolve[networks.size() -i -1] = networks.get(i).getResolve();
                SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");//设置日期格式
                String date = df.format(networks.get(i).getTime());
                netNowTime[networks.size() -i -1] = date;
            }
            Network.NetData netData = new Network.NetData();
            netData.setNetName(networks.get(0).getNetName());
            netData.setNetId(netId.get(k));
            netData.setNetSend(netSend);
            netData.setNetResolve(netResolve);
            netData.setNetNowTime(netNowTime);
            netData.setNetIpv4(networks.get(0).getNetIpv4());
            netData.setNetIpv6(networks.get(0).getNetIpv6());
            netDataList.add(netData);
        }
        Network network = new Network();
        network.setNodeId(nodeId);
        network.setNetData(netDataList);


        return network;
    }

}
