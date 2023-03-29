package com.hw.hwbackend.entity;

import lombok.Data;

import java.util.ArrayList;

@Data
public class PgNode {
    private int nodeId;
    private ArrayList<PgNode.PgDisk> diskList;
    @Data
    public static class PgDisk{
        private int diskId;
        private ArrayList<Pg> pgList;
    }
}
