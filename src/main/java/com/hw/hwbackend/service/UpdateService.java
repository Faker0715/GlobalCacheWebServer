package com.hw.hwbackend.service;

import com.hw.hwbackend.dataservice.PgListData;
import com.hw.hwbackend.dataservice.PtListData;
import com.hw.hwbackend.entity.*;
import com.hw.hwbackend.saveservice.PgListSave;
import com.hw.hwbackend.saveservice.PtListSave;
import com.hw.hwbackend.util.ResponseResult;
import com.hw.hwbackend.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//Pt Pg信息的业务类
@Service
public class UpdateService {
    @Autowired
    private PtListSave ptListSave;
    @Autowired
    private PgListSave pgListSave;
    @Autowired
    private PtListData ptListData;
    @Autowired
    private PgListData pgListData;
    //获取Pt信息
    public ResponseResult getPtUpdate(String token){

            //重新向集群查询 并保存信息
            ptListSave.PtListSchedule();
            List<PtList> ptLists = new ArrayList<>();
            ptLists = ptListData.findptList();

            //获取当前已经连接的节点
            UserHolder userHolder = UserHolder.getInstance();
            ArrayList<Integer> nodes = userHolder.getIprelation().getNodes();
            ArrayList<PtNode> nodelist = new ArrayList<>();
            //封装成前端所需的数据格式
            for(int i = 0;i < nodes.size();++i){
                PtNode ptNode = new PtNode();
                ptNode.setNodeId(nodes.get(i));
                ArrayList<PtNode.PtDisk> diskList = new ArrayList<>();

                List<Pt> pts = ptListData.findptNodeList(nodes.get(i)).get(0).getPtArrayList();

                //获取DiskId
                ArrayList<Integer> disks = new ArrayList<>();
                for (int j = 0; j < pts.size(); j++) {
                    if (!disks.contains(pts.get(j).getDiskId())){
                        disks.add(pts.get(j).getDiskId());
                    }
                }
                //根据diskid将数据分类
                for (int j = 0; j < disks.size(); j++) {
                    PtNode.PtDisk ptDisk = new PtNode.PtDisk();
                    ptDisk.setDiskId(disks.get(j));
                    ArrayList<Pt> ptArrayList = new ArrayList<>();
                    for (int k = 0; k < pts.size(); k++) {
                        if (pts.get(k).getDiskId() == disks.get(j)){
                            ptArrayList.add(pts.get(k));
                        }
                    }
                    ptDisk.setPtList(ptArrayList);
                    diskList.add(ptDisk);
                }
                ptNode.setDiskList(diskList);
                nodelist.add(ptNode);
            }

            Map map = new HashMap<>();
            map.put("nodeList",nodelist);
            map.put("ptList",ptLists.get(0).getPtArrayList());
            return new ResponseResult<Map<String, Object>>(map);
    }

    public ResponseResult getPgUpdate(String token){
            //重新向集群查询 并保存信息
            pgListSave.PgListSchedule();
            List<PgList> pgLists = new ArrayList<>();
            pgLists = pgListData.findpgList();

            UserHolder userHolder = UserHolder.getInstance();
            ArrayList<Integer> nodes = userHolder.getIprelation().getNodes();
            ArrayList<PgNode> nodelist = new ArrayList<>();
            //封装成前端所需的数据格式
            for(int i = 0;i < nodes.size();++i){
                PgNode pgNode = new PgNode();
                pgNode.setNodeId(nodes.get(i));
                ArrayList<PgNode.PgDisk> diskList = new ArrayList<>();

                List<Pg> pgs = pgListData.findpgNodeList(nodes.get(i)).get(0).getPgArrayList();

                //获取DiskId
                ArrayList<Integer> disks = new ArrayList<>();
                for (int j = 0; j < pgs.size(); j++) {
                    if (!disks.contains(pgs.get(j).getDiskId())){
                        disks.add(pgs.get(j).getDiskId());
                    }
                }
                //根据diskid将数据分类
                for (int j = 0; j < disks.size(); j++) {
                    PgNode.PgDisk pgDisk = new PgNode.PgDisk();
                    pgDisk.setDiskId(disks.get(j));
                    ArrayList<Pg> pgArrayList = new ArrayList<>();
                    for (int k = 0; k < pgs.size(); k++) {
                        if (pgs.get(k).getDiskId() == disks.get(j)){
                            pgArrayList.add(pgs.get(k));
                        }
                    }
                    pgDisk.setPgList(pgArrayList);
                    diskList.add(pgDisk);
                }
                pgNode.setDiskList(diskList);
                nodelist.add(pgNode);
            }

        Map map = new HashMap<>();
        map.put("nodeList",nodelist);
        map.put("pgList",pgLists.get(0).getPgArrayList());
        return new ResponseResult<Map<String, Object>>(map);
    }

}
