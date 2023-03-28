package com.hw.hwbackend.entity;


import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection="Network")//对应数据库的表Memory
@CompoundIndex( def = "{'userid': 1, 'nickname': -1}")
public class Network {


    private long id;



    private String netName;
    private int netId;

    private double send;

    private double resolve;

    private String netIpv4;

    private String netIpv6;

    private double netRatio;

    private List<NetData> netData;

    private long time;

    private int nodeId;



    @Data
    public static class NetData{

        private String netName;
        private int netId;
        private Double[] netSend;
        private Double[] netResolve;
        private String[] netNowTime;

        private String netIpv4;

        private String netIpv6;
    }

}
