package com.hw.hwbackend.entity;
import com.hw.globalcachesdk.entity.CacheDiskInfo;
import com.hw.globalcachesdk.entity.DiskInfo;

import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data

@Document(collection="Disk")
@CompoundIndex( def = "{'userid': 1, 'nickname': -1}")
public class Disk
{

    private long id;
    private Disk.DiskList diskList;

    private List<Disk.DiskInfo> diskInfo;

    private List<Disk.DiskIORatio> diskIORatio;
    private int nodeId;

    private long time;



    @Data
    public static class DiskIo {

        private String name;
        private float readRatio;

        private float writeRatio;
        private Time diskNowTime;

        private long time;

        private int nodeId;
    }

    @Data
    public static class DiskList {
        List<Disk.DiskInfo> diskInfo;
        List<Disk.DiskIo> diskIORatio;

    }

    @Data
    public static class DiskInfo{
        private String diskName;
        private int diskId;
        private double diskCapacity;
        private String diskType;
        private String diskSn;
        private long time;
        private CacheDiskInfo.CacheDiskState state;
        private int nodeId;

    }

    @Data
    public static class DiskIORatio{

        private String name;

        private Float[] readRatio;

        private Float[] writeRatio;

        private String[] diskNowTime;

    }

}
