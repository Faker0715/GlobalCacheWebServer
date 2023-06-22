package com.hw.hwbackend.entity;
import com.hw.globalcachesdk.entity.CacheDiskInfo;

import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data

@Document(collection="Disk")
@CompoundIndex( def = "{'userid': 1, 'nickname': -1}")
public class Disk
{

    private long id;
    private int nodeId;
    private Map<String,AllDiskInfo> diskInfoMap;
    private long time;

    @Data
    public static class AllDiskInfo{
        private DiskBasicInfo diskBasicInfo;
        private DiskIoInfoBase diskIORatio;
    }


    @Data
    public static class DiskIoInfoBase {
        private String name;
        private float readRatio;
        private float writeRatio;
        private long time;
        private Float[] readRatioArr;
        private Float[] writeRatioArr;
        private String[] diskNowTimeArr;
    }

    @Data
    public static class DiskBasicInfo{
        private String name;
        private double diskCapacity;
        private String diskType;
        private List<CacheInfo> cacheInfolist;
    }

    @Data
    public static class CacheInfo{
        private String CacheName;
        private int diskId;
        private String diskSn;
        private CacheDiskInfo.CacheDiskState state;
    }

}
