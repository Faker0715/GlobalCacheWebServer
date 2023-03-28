package com.hw.hwbackend.entity;

import lombok.Data;

import java.util.ArrayList;

@Data
public class PtNode {

    private int nodeId;
    private ArrayList<PtDisk> diskList;
    @Data
    public static class PtDisk{
        private int diskId;
        private ArrayList<Pt> ptList;
    }
}
