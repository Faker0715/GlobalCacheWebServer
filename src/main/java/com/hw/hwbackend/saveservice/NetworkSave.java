package com.hw.hwbackend.saveservice;

import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.StatusCode;
import com.hw.globalcachesdk.entity.DynamicNetInfo;
import com.hw.globalcachesdk.entity.StaticNetInfo;
import com.hw.globalcachesdk.exception.GlobalCacheSDKException;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import com.hw.hwbackend.dataservice.NetworkData;
import com.hw.hwbackend.entity.Network;
import com.hw.hwbackend.util.UserHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//从集群获取Network数据 存入数据库
@Service
public class NetworkSave {
    @Autowired
    private NetworkData networkData;

    private static Logger log = LoggerFactory.getLogger(NetworkSave.class);
    public void NetworkSchedule() {
        long time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();

        //获取连接当前节点信息
        UserHolder userHolder = UserHolder.getInstance();
        ArrayList<String> hosts = userHolder.getIprelation().getIps();
        Map<String,Integer> ipmap = userHolder.getIprelation().getIpMap();

        Map<String, StaticNetInfo> staticNetInfomap = userHolder.getIprelation().getStaticNetInfomap();

        Map<String,DynamicNetInfo> dynamicNetInfomap = new HashMap<>();
        //获取DynamicNet数据
        try {
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryDynamicNetInfo(hosts).entrySet()) {
                log.info("networksave: querydynamicNetInfo : "+ entry.getValue().getStatusCode());
                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                    log.info("networksave-querydynamicnetinfo-data: "+ (DynamicNetInfo)(entry.getValue().getData()));
                    dynamicNetInfomap.put(entry.getKey(),(DynamicNetInfo) entry.getValue().getData());
                }
            }
        } catch (GlobalCacheSDKException e) {
            System.out.println("接口调用失败");
            e.printStackTrace();
        }


        //获取StaticNet数据
        for (int i = 0; i < hosts.size(); i++) {
            Network network = new Network();

            List<Network.NetData> netDatalist = new ArrayList<>();
            ArrayList<StaticNetInfo.InterfaceInfo> staticnetList = staticNetInfomap.get(hosts.get(i)).getInterfaceInfoList();
            ArrayList<DynamicNetInfo.InterfaceInfo> dynamicnetList = dynamicNetInfomap.get(hosts.get(i)).getInterfaceInfos();
            for (int k = 0; k < staticnetList.size() ; k++) {
                StaticNetInfo.InterfaceInfo staticinterfaceInfo = staticnetList.get(k);
                if(staticinterfaceInfo.getIpv4().equals("") || staticinterfaceInfo.getIpv4().contains("127.0.0.1")){
                    continue;
                }
                for(int j = 0; j < dynamicnetList.size(); ++j){
                    DynamicNetInfo.InterfaceInfo dynamicinterfaceInfo = dynamicnetList.get(j);
                    //StaticNet 和 DynamicNet信息合并
                    if(dynamicinterfaceInfo.getName().equals(staticinterfaceInfo.getName())){
                        //封装
                        Network.NetData netData = new Network.NetData();
                        netData.setNetName(staticinterfaceInfo.getName());
                        //处理Ipv4 Ipv6地址
                        String ipv4 = staticinterfaceInfo.getIpv4();
                        String ipv6 = staticinterfaceInfo.getIpv6();
                        String []str1 = ipv4.split("/");
                        String []str2 = ipv6.split("/");
                        netData.setNetIpv4(str1[0]);
                        netData.setNetIpv6(str2[0]);
                        netData.setNetId(i);
                        netData.setNetRatio(1);
                        netData.setResolve(dynamicinterfaceInfo.getRxkBs());
                        netData.setSend(dynamicinterfaceInfo.getTxkBs());
                        netDatalist.add(netData);
                        //保存
                    }
                }
            }
            network.setTime(time);
            network.setNodeId(ipmap.get(hosts.get(i)));
            network.setNetData(netDatalist);
            String id = ipmap.get(hosts.get(i)) + "1" + time;
            network.setId(Long.parseLong(id));
            networkData.saveNetwork(network);

        }

    }

}
