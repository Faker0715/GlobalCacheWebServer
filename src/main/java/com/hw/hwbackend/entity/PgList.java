package com.hw.hwbackend.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;

@Data
@Document(collection = "PgList")
@CompoundIndex(def = "{'userid': 1, 'nickname': -1}")
public class PgList {
    private long id;
    private long time;
    private ArrayList<Pg> pgArrayList = new ArrayList<>();

}