package com.hw.hwbackend.entity;

public class Ceph {
    private String ceph;
    private String ip;

    public String getCeph() {
        return ceph;
    }

    public void setCeph(String ceph) {
        this.ceph = ceph;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Ceph(String ceph, String ip) {
        this.ceph = ceph;
        this.ip = ip;
    }

    @Override
    public String toString() {
        return "Ceph{" +
                "ceph='" + ceph + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }



}
