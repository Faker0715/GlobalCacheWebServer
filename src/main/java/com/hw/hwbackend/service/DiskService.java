package com.hw.hwbackend.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hw.hwbackend.dataservice.DiskData;
import com.hw.hwbackend.dto.WebsocketDTO;
import com.hw.hwbackend.entity.Cpu;
import com.hw.hwbackend.entity.Disk;
import com.hw.hwbackend.security.WebSocketHandlerListenterImpl;
import com.hw.hwbackend.util.UserHolder;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hw.hwbackend.util.RedisConstants.CACHE_Disk_KEY;
import static com.hw.hwbackend.util.RedisConstants.CACHE_Disk_TIMEOUT;


//Disk信息的业务类
@Service
public class DiskService {

    private static Logger log = LoggerFactory.getLogger(DiskService.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DiskData diskData;

    public void sendMsg() {
        //将数据封装成json
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("url", "getDiskData");
        //根据websoket连接 判断
        for (Map.Entry<String, Channel> entry : WebSocketHandlerListenterImpl.chanelIdMap.entrySet()) {
            WebsocketDTO wsdto = WebSocketHandlerListenterImpl.WebsocketMap.get(entry.getKey());
            if(wsdto.getUrl().equals("/getDiskData") && wsdto.getParams().getNodeId() != -1){
                jsonObject.put("params", JSONObject.toJSON(getDiskData(wsdto.getParams().getNodeId())));
                jsonObject.put("token",wsdto.getParams().getToken());
                //发送给前端
                entry.getValue().writeAndFlush(new TextWebSocketFrame(jsonObject.toJSONString()));
            }
        }

    }

    public Disk getDiskData(int nodeId) {

        //创建redis key
        String key = CACHE_Disk_KEY + nodeId;
        String json = stringRedisTemplate.opsForValue().get(key);
        List<Disk> disklist = new ArrayList<>();
        //如果redis有数据直接从中取
        if(StrUtil.isNotBlank(json)){
            disklist = JSON.parseArray(json,Disk.class);
        }else{
            //没有就从数据库查询
            disklist = diskData.findDiskbyNodeId(nodeId);
            if(BeanUtil.isNotEmpty(disklist))
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(disklist),CACHE_Disk_TIMEOUT, TimeUnit.SECONDS);
        }
        //将数据封装成前端所需的格式
        if(disklist.size() == 0)
            return null;
        Disk disk = new Disk();
        disk.setNodeId(disklist.get(0).getNodeId());
        disk.setDiskInfo(disklist.get(0).getDiskList().getDiskInfo());
        disk.setTime(disklist.get(0).getTime());
        List<Disk.DiskIORatio> diskIORatios = new ArrayList<>();
        List<Disk.DiskIo> diskIoList = disklist.get(0).getDiskList().getDiskIORatio();
        for (int j = 0; j < diskIoList.size(); j++) {
            Disk.DiskIORatio diskIORatio = new Disk.DiskIORatio();
            String name = diskIoList.get(j).getName();
            Float[] readRatio = new Float[disklist.size()];
            Float[] writeRatio = new Float[disklist.size()];
            String[] diskNowTime = new String[disklist.size()];
            for (int i = 0; i < disklist.size(); i++) {
                List<Disk.DiskIo> diskIoList2 = disklist.get(i).getDiskList().getDiskIORatio();
                for (Disk.DiskIo diskIo : diskIoList2){
                    if (diskIo.getName().equals(name)){
                        readRatio[disklist.size()-i-1] = diskIo.getReadRatio();
                        writeRatio[disklist.size()-i-1] = diskIo.getWriteRatio();
                        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");//设置日期格式
                        String date = df.format(diskIo.getTime());
                        diskNowTime[disklist.size()-i-1] = date;
                    }
                }
            }
            diskIORatio.setName(name);
            diskIORatio.setReadRatio(readRatio);
            diskIORatio.setWriteRatio(writeRatio);
            diskIORatio.setDiskNowTime(diskNowTime);
            diskIORatios.add(diskIORatio);
        }
        disk.setDiskIORatio(diskIORatios);
        return disk;
    }
}
