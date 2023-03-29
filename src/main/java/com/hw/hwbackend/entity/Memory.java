package com.hw.hwbackend.entity;



import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection="Memory")//对应数据库的表Memory
@CompoundIndex( def = "{'userid': 1, 'nickname': -1}")
public class Memory {

    public Memory(){}
    private long id;
    private long time;
    private double memoryUsing;
    private double memoryUseable;
    private double memoryCache;
    private double memoryRatio;
    private Double[] memoryRatioList;
    private String[] memoryNowTime;
    private int nodeId;




}