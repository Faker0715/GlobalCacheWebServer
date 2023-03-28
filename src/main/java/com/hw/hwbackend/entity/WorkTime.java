package com.hw.hwbackend.entity;

import lombok.Data;

@Data
public class WorkTime {
    public WorkTime(int hh, int mm, int dd) {
        this.hh = hh;
        this.mm = mm;
        this.dd = dd;
    }
    private int hh;
    private int mm;
    private int dd;
}
