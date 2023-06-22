package com.hw.hwbackend.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import com.hw.hwbackend.dataservice.DiskData;
import com.hw.hwbackend.dto.WebsocketDTO;
import com.hw.hwbackend.entity.Disk;
import com.hw.hwbackend.security.WebSocketHandlerListenterImpl;
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
import java.util.HashMap;
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
        WebSocketHandlerListenterImpl webSocketHandlerListenter = WebSocketHandlerListenterImpl.getInstance();
        //根据websoket连接 判断
        for (Map.Entry<String, Channel> entry : webSocketHandlerListenter.chanelIdMap.entrySet()) {
            WebsocketDTO wsdto = webSocketHandlerListenter.WebsocketMap.get(entry.getKey());
            if (wsdto == null) {
                continue;
            }
            if (wsdto.getUrl().equals("/getDiskData") && wsdto.getParams().getNodeId() != -1) {
                jsonObject.put("params", JSONObject.toJSON(getDiskData(wsdto.getParams().getNodeId())));
                jsonObject.put("token", wsdto.getParams().getToken());
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
        if (StrUtil.isNotBlank(json)) {
            disklist = JSON.parseArray(json, Disk.class);
        } else {
            //没有就从数据库查询
            disklist = diskData.findDiskbyNodeId(nodeId);
            if (BeanUtil.isNotEmpty(disklist))
                stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(disklist), CACHE_Disk_TIMEOUT, TimeUnit.SECONDS);
        }
        //将数据封装成前端所需的格式
        if (disklist.size() == 0)
            return null;

        List<String> allDiskName = new ArrayList<>();

        Map<String, Disk.AllDiskInfo> dmap = disklist.get(0).getDiskInfoMap();
        for (Map.Entry<String, Disk.AllDiskInfo> entry : dmap.entrySet()) {
            allDiskName.add(entry.getKey());
        }

        Map<String, Disk.AllDiskInfo> returnmap = new HashMap<>();


        for (String dName : allDiskName) {

            Disk.AllDiskInfo eachDiskInfo = new Disk.AllDiskInfo();

            Float[] readRatio = new Float[disklist.size()];
            Float[] writeRatio = new Float[disklist.size()];
            String[] diskNowTime = new String[disklist.size()];

            // 找出相同的io
            List<Disk.DiskIoInfoBase> diskIoInfoBaseList = new ArrayList<>();
            for (Disk disk : disklist) {
                Map<String, Disk.AllDiskInfo> diskInfoMap = disk.getDiskInfoMap();
                Disk.AllDiskInfo allDiskInfo = diskInfoMap.get(dName);
                diskIoInfoBaseList.add(allDiskInfo.getDiskIORatio());
            }
            // 对io进行排序
            Disk.DiskIoInfoBase diskIoInfoBase = new Disk.DiskIoInfoBase();
            diskIoInfoBase.setName(dName);
            for (int i = 0; i < diskIoInfoBaseList.size(); i++) {
                readRatio[disklist.size() - i - 1] = diskIoInfoBaseList.get(i).getReadRatio();
                writeRatio[disklist.size() - i - 1] = diskIoInfoBaseList.get(i).getWriteRatio();
                SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");//设置日期格式
                String date = df.format(diskIoInfoBaseList.get(i).getTime());
                diskNowTime[disklist.size() - i - 1] = date;
            }
            diskIoInfoBase.setReadRatioArr(readRatio);
            diskIoInfoBase.setWriteRatioArr(writeRatio);
            diskIoInfoBase.setDiskNowTimeArr(diskNowTime);

            eachDiskInfo.setDiskIORatio(diskIoInfoBase);
            eachDiskInfo.setDiskBasicInfo(disklist.get(0).getDiskInfoMap().get(dName).getDiskBasicInfo());

            returnmap.put(dName, eachDiskInfo);
        }
        Disk disk = new Disk();
        disk.setNodeId(nodeId);
        disk.setTime(disklist.get(0).getTime());
        disk.setDiskInfoMap(returnmap);
        return disk;
    }
}
