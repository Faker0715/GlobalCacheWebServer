package com.hw.hwbackend.entity;

import com.hw.globalcachesdk.entity.PtInfo;

import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;

@Data
@Document(collection="PtList")
@CompoundIndex( def = "{'userid': 1, 'nickname': -1}")
public class PtList {
    private long id;
    private long time;
    private ArrayList<Pt> ptArrayList;
}
