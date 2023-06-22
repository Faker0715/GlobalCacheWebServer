package com.hw.hwbackend.saveservice;

import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.StatusCode;
import com.hw.globalcachesdk.entity.CacheDiskInfo;
import com.hw.globalcachesdk.entity.DiskInfo;
import com.hw.globalcachesdk.entity.DiskIoInfo;
import com.hw.globalcachesdk.exception.GlobalCacheSDKException;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import com.hw.hwbackend.dataservice.DiskData;
import com.hw.hwbackend.entity.AutoList;
import com.hw.hwbackend.entity.Disk;
import com.hw.hwbackend.entity.Time;
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

//从集群获取Disk数据 存入数据库
@Service
public class DiskSave {

    @Autowired
    private DiskData diskData;

    private static Logger log = LoggerFactory.getLogger(DiskSave.class);

    public void DiskSchedule() {

        long stime = System.currentTimeMillis();
        //获取连接当前节点信息
        UserHolder userHolder = UserHolder.getInstance();
        ArrayList<String> hosts = userHolder.getIprelation().getIps();
        Map<String, Integer> ipmap = userHolder.getIprelation().getIpMap();

        Map<Integer, ArrayList<Integer>> disksmap = userHolder.getIprelation().getDisks();

        String ceph1 = userHolder.getCeph1();
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        long time = zonedDateTime.toInstant().toEpochMilli();
        int hour = zonedDateTime.getHour();
        int minute = zonedDateTime.getMinute();
        int second = zonedDateTime.getSecond();
        Map<String, DiskIoInfo> diskIoInfomap = new HashMap<>();
        CacheDiskInfo cacheDiskInfo = new CacheDiskInfo();
        try {
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryCacheDiskInfo(ceph1).entrySet()) {
                log.info("disksave-querycachediskinfo: " + entry.getValue().getStatusCode());
                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                    log.info("disksave-querycachediskinfo-data: " + (CacheDiskInfo) (entry.getValue().getData()));
                    cacheDiskInfo = (CacheDiskInfo) (entry.getValue().getData());
                }
            }
        } catch (GlobalCacheSDKException e) {
            System.out.println("接口调用失败");
            e.printStackTrace();
        }
        try {
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryDiskIoInfo(hosts).entrySet()) {
                log.info("disksave-querydiskioinfo: " + entry.getValue().getStatusCode());
                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                    log.info("disksave-querydiskioinfo-data: " + (DiskIoInfo) (entry.getValue().getData()));
                    diskIoInfomap.put(entry.getKey(), (DiskIoInfo) (entry.getValue().getData()));
                }
            }
        } catch (GlobalCacheSDKException e) {
            System.out.println("接口调用失败");
            e.printStackTrace();
        }
        try {
            for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryDiskInfo(hosts).entrySet()) {
                DiskInfo diskInfo = new DiskInfo();
                if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                    diskInfo = (DiskInfo) entry.getValue().getData();
                    ArrayList<DiskInfo.Disk> disksList = diskInfo.getDisksList();

                    List<DiskIoInfo.DiskIo> diskios = diskIoInfomap.get(entry.getKey()).getDiskIoList();
                    List<CacheDiskInfo.CacheDisk> cachediskios = cacheDiskInfo.getDiskList();
                    //封装diskIoList
                    List<Disk.DiskIoInfoBase> diskIoList = new ArrayList<>();

                    //封装diskBasicInfo
                    ArrayList<Integer> cacheDiskIds = disksmap.get(ipmap.get(entry.getKey()));
                    Map<String, Disk.AllDiskInfo> diskInfoMap = new HashMap<>();
                    // 封装data
                    for (DiskInfo.Disk sdkdisk : disksList) {
                        Disk.DiskBasicInfo basicDisk = new Disk.DiskBasicInfo();
                        basicDisk.setName(sdkdisk.getName());
                        basicDisk.setDiskType("ROTA");
                        basicDisk.setDiskCapacity(sdkdisk.getCapacity());
                        if (sdkdisk.getType() == DiskInfo.DiskType.NVME) {
                            basicDisk.setName(sdkdisk.getName());
                            basicDisk.setDiskType("SSD");
                            basicDisk.setDiskCapacity(sdkdisk.getCapacity());
                            List<Disk.CacheInfo> cacheInfolist = new ArrayList<>();
                            // 封装cache
                            for (CacheDiskInfo.CacheDisk cacheDisk : cachediskios) {
                                if(cacheDiskIds.contains(cacheDisk.getDiskId()) && cacheDisk.getDiskName().contains(sdkdisk.getName())){
                                    Disk.CacheInfo cacheInfo = new Disk.CacheInfo();
                                    cacheInfo.setDiskId(cacheDisk.getDiskId());
                                    cacheInfo.setCacheName(cacheDisk.getDiskName());
                                    cacheInfo.setDiskSn(cacheDisk.getDiskSn());
                                    cacheInfo.setState(cacheDisk.getState());
                                    cacheInfolist.add(cacheInfo);
                                }
                            }
                            basicDisk.setCacheInfolist(cacheInfolist);
                        }
                        Disk.DiskIoInfoBase disk = new Disk.DiskIoInfoBase();
                        for (DiskIoInfo.DiskIo diskio : diskios) {
                            if(diskio.getDiskName().equals(sdkdisk.getName())){
                                disk.setName(diskio.getDiskName());
                                disk.setReadRatio(diskio.getKbRead());
                                disk.setWriteRatio(diskio.getKbWrite());
                                disk.setTime(time);
                            }
                        }

                        Disk.AllDiskInfo allDiskInfo = new Disk.AllDiskInfo();
                        allDiskInfo.setDiskBasicInfo(basicDisk);
                        allDiskInfo.setDiskIORatio(disk);
                        diskInfoMap.put(sdkdisk.getName(),allDiskInfo);
                    }
                    Disk disk = new Disk();
                    disk.setTime(time);
                    disk.setNodeId(ipmap.get(entry.getKey()));
                    String id = ipmap.get(entry.getKey()) + "1" + time;
                    disk.setId(Long.parseLong(id));
                    disk.setDiskInfoMap(diskInfoMap);
                    diskData.saveDisk(disk);
                    log.info("disksave-disk: " + disk.toString());
                    //保存
                } else {
                    System.out.println(entry.getValue().getStatusCode());
                }
            }
        } catch (GlobalCacheSDKException e) {
            System.out.println("接口调用失败");
            e.printStackTrace();
        }
        long etime = System.currentTimeMillis();
        // 计算执行时间
        System.out.printf("disksave time：%d ms.", (etime - stime));

    }

}
