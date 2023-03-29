package com.hw.hwbackend.entity;

import com.hw.globalcachesdk.entity.PgInfo;
import lombok.Data;

import java.util.ArrayList;

@Data
public class Pg {
    private int pgId;
    private int bv;
    private PgInfo.PgState state;
    private int masterNode;
    private int masterDisk;
    private int copyNum;
    private int nodeId;
    private int diskId;
    private ArrayList<CopyInfo> CopyInfos = new ArrayList<>();
    @Data
    public static class CopyInfo {
        private int nodeId;
        private int diskId;
        private PgInfo.CopyState copyState;
    }
}