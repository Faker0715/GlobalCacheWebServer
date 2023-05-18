package com.hw.hwbackend.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AutoList {
    private List<AutoEntity> autoEntityArrayList = new ArrayList<>();

    private String pnet;

    private String cnet;
    private String pubMask;

    private String cluMask;

    private Integer ptNum;
    private Integer pgNum;

    private String password;


    @Data
    public static class AutoEntity{
        String name;

        boolean ceph;
        boolean client;
        boolean ceph1;
        String roleName;
        String localIPv4;
        String clusterIPv4;
        boolean isConnected;
        boolean isCpu;
        boolean isMemory;

        ArrayList<DataDisk> dataDisk = new ArrayList<>();

        ArrayList<CacheDisk> cacheDisk = new ArrayList<>();

        ArrayList<DataDisk> dataList = new ArrayList<>();
        ArrayList<CacheDisk> cacheList = new ArrayList<>();

        public boolean Ceph() {
            return ceph;
        }

        public void setCeph(boolean ceph) {
            this.ceph = ceph;
        }

        public boolean Client() {
            return client;
        }

        public void setClient(boolean client) {
            this.client = client;
        }

        public boolean Ceph1() {
            return ceph1;
        }

        public void setCeph1(boolean ceph1) {
            this.ceph1 = ceph1;
        }


        public boolean getisConnected() {
            return isConnected;
        }

        public void setisConnected(boolean connected) {
            isConnected = connected;
        }

        public boolean getisCpu() {
            return isCpu;
        }

        public void setisCpu(boolean cpu) {
            isCpu = cpu;
        }

        public boolean getisMemory() {
            return isMemory;
        }

        public void setisMemory(boolean memory) {
            isMemory = memory;
        }



        @Data
        public static class DataDisk {
            int id;
            String name;
            String type;
        }
        @Data
        public static class CacheDisk{
            int id;
            String name;
            String type;
        }
    }


}
