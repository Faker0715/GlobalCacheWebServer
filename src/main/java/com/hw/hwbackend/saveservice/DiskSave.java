package com.hw.hwbackend.saveservice;

import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.StatusCode;
import com.hw.globalcachesdk.entity.CacheDiskInfo;
import com.hw.globalcachesdk.entity.DiskIoInfo;
import com.hw.globalcachesdk.exception.GlobalCacheSDKException;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import com.hw.hwbackend.dataservice.DiskData;
import com.hw.hwbackend.entity.Disk;
import com.hw.hwbackend.entity.Time;
import com.hw.hwbackend.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
//从集群获取Disk数据 存入数据库
@Service
public class DiskSave {

        @Autowired
        private DiskData diskData;

        public void DiskSchedule() {
            //获取连接当前节点信息
            UserHolder userHolder = UserHolder.getInstance();
            ArrayList<String> hosts = userHolder.getIprelation().getIps();
            Map<String,Integer> ipmap = userHolder.getIprelation().getIpMap();
            ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
            long time = zonedDateTime.toInstant().toEpochMilli();
            int hour = zonedDateTime.getHour();
            int minute = zonedDateTime.getMinute();
            int second = zonedDateTime.getSecond();
            DiskIoInfo diskIoInfo = new DiskIoInfo();
            CacheDiskInfo cacheDiskInfo = new CacheDiskInfo();
            //获取CacheDisk数据
            for (int i = 0; i < hosts.size(); i++) {
                try {
                    for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryCacheDiskInfo(hosts.get(i)).entrySet()) {
                        if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                            cacheDiskInfo = (CacheDiskInfo) (entry.getValue().getData());
                        }
                    }
                } catch (GlobalCacheSDKException e) {
                    System.out.println("接口调用失败");
                    e.printStackTrace();
                }
                //先获取diskid

                ArrayList<String> hosts1 = new ArrayList<>();
                hosts1.add(hosts.get(i));
                //获取DiskIo信息
                try {
                    for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryDiskIoInfo(hosts1).entrySet()) {
                        if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {

                            diskIoInfo = (DiskIoInfo) (entry.getValue().getData());
                        }
                    }
                } catch (GlobalCacheSDKException e) {
                    System.out.println("接口调用失败");
                    e.printStackTrace();
                }
                List<DiskIoInfo.DiskIo> diskios = diskIoInfo.getDiskIoList();
                List<Disk.DiskIo> diskIoList = new ArrayList<>();
                //封装diskIoList
                for (DiskIoInfo.DiskIo diskio : diskios) {
                    if (diskio.getDiskName().substring(0,1).equals("n")){
                        Disk.DiskIo d = new Disk.DiskIo();

                        d.setDiskNowTime(new Time(hour, minute, second));
                        d.setName(diskio.getDiskName());
                        d.setNodeId(ipmap.get(hosts1.get(0)));
                        d.setReadRatio(diskio.getKbRead());
                        d.setWriteRatio(diskio.getKbWrite());
                        d.setTime(time);
                        diskIoList.add(d);
                    }
                }
                //封装diskInfoList
                List<Disk.DiskInfo> diskInfoList = new ArrayList<>();
                for (CacheDiskInfo.CacheDisk cacheDisk : cacheDiskInfo.getDiskList()) {
                    Disk.DiskInfo d = new Disk.DiskInfo();

                    d.setDiskId(cacheDisk.getDiskId());
                    d.setDiskName(cacheDisk.getDiskName());
                    d.setDiskType("SSD");
                    d.setDiskCapacity((double)cacheDisk.getCapacity()*1.0/(1024*1024));
                    d.setDiskSn(cacheDisk.getDiskSn());
                    d.setState(cacheDisk.getState());
                    d.setNodeId(ipmap.get(hosts.get(i)));
                    d.setTime(time);

                    diskInfoList.add(d);
                }

                Disk.DiskList diskList = new Disk.DiskList();

                diskList.setDiskInfo(diskInfoList);
                diskList.setDiskIORatio(diskIoList);
                //封装disk
                Disk disk = new Disk();
                disk.setDiskList(diskList);
                disk.setTime(time);
                disk.setNodeId(ipmap.get(hosts.get(i)));
                String id = ipmap.get(hosts.get(i)) + "1" + time;
                disk.setId(Long.parseLong(id));
                diskData.saveDisk(disk);

            }

        }

}
