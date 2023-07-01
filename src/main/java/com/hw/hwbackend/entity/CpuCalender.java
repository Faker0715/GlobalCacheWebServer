package com.hw.hwbackend.entity;


import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;

@Data
@Document(collection="CpuCalender")
@CompoundIndex( def = "{'userid': 1, 'nickname': -1}")
public class CpuCalender {

    private long id;
    private ArrayList<CpuNode> cpuNodeArrayList;
    private long time;
    @Data
    public static class CpuNode{

        public enum NodeState {
            // 节点无效
            NODE_STATE_INVALID,
            // 节点正在启动
            NODE_STATE_UP,
            // 节点启动完成
            NODE_STATE_RUNNING,
            // 节点处于down状态，不能正常工作
            NODE_STATE_DOWN,
            // 表示节点在集群中
            NODE_STATE_IN,
            // 表示节点不在集群中，等待扩容后加入集群
            NODE_STATE_OUT
        }
        private int nodeId;
        private double nodeValue;
        private NodeState isIn;
        private NodeState isRunning;
        private Boolean isOnline;
    }

}
