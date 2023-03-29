package com.hw.hwbackend.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;

@Data
@Document(collection="HealthList")
@CompoundIndex( def = "{'userid': 1, 'nickname': -1}")
public class HealthList {

    private long id;
    private String clusterState;
    private ArrayList<Health> healthArrayList;
    private long time;

    @Data
    public static class Health{
        private String abnType;
        private String abnLevel;
        private String abnDetails;
        private String abnTime;
    }

}
