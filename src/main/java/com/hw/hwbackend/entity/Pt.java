package com.hw.hwbackend.entity;

import com.hw.globalcachesdk.entity.PtInfo;
import lombok.Data;

import java.util.ArrayList;

@Data
public class Pt {
    private int nodeId;
    private int diskId;
    private int ptId;
    private int bv;
    private PtInfo.PtState state;
    private ArrayList<ArrayList<Integer>> ptInfo = new ArrayList<>();
    private int indexNode;
    private IoInfo ioInfo;
    private String NodeIp;
    @Data
    public static class IoInfo {
        private int ioCount;
        private int readCount;
        private long readSize;
        private int writeCount;
        private long writeSize;

    }
}
