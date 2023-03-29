package com.hw.hwbackend.entity;


import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data


//mongodb***********************************
@Document(collection = "Cpu")
@CompoundIndex( def = "{'userid': 1, 'nickname': -1}")
//复合索引
public class Cpu {

    private long id;
    private long time;
    private double cpuUse;
    private WorkTime workingTime;
    private CpuRatio cpuRatio;
    private int nodeId;


    @Data
    public static class CpuRatio{
        private String[] cpuTime;

        private Double[] cpuUse;

    }


}
