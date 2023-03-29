package com.hw.hwbackend.entity;


import lombok.Data;

@Data
public class Time {
    public Time(){

    }
    public Time(int hh, int mm, int ss) {
        this.hh = hh;
        this.mm = mm;
        this.ss = ss;
    }

    private int hh;
    private int mm;
    private int ss;
}
