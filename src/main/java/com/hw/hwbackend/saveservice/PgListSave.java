package com.hw.hwbackend.saveservice;

import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.StatusCode;
import com.hw.globalcachesdk.entity.PgInfo;
import com.hw.globalcachesdk.exception.GlobalCacheSDKException;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import com.hw.globalcachesdk.executor.RegisterExecutor;
import com.hw.hwbackend.dataservice.PgListData;
import com.hw.hwbackend.entity.Pg;
import com.hw.hwbackend.entity.PgList;
import com.hw.hwbackend.util.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;


import static java.lang.Thread.sleep;
//从集群获取Pg数据 存入数据库
@Service
public class PgListSave {

    @Autowired
    private PgListData pgListData;


    public void PgListSchedule() {

        String cephip = UserHolder.getInstance().getCeph1();
        long time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();

        //获取连接当前节点信息
        UserHolder userHolder = UserHolder.getInstance();
        ArrayList<String> hosts = userHolder.getIprelation().getIps();
        Map<String, Integer> ipmap = userHolder.getIprelation().getIpMap();

        int tryTimes = 5;
        int oldTimeout = 0;
        try {
            // 记录当前接口默认超时等待时间
            oldTimeout = GlobalCacheSDK.getCommandConf(RegisterExecutor.QUERY_ALL_PG_INFO).getTimeout();
        } catch (GlobalCacheSDKException e) {
            System.out.println("获取执行时间失败");
            e.printStackTrace();
        }
        // 时长增长因子
        double increaseFactor = 1.5f;
        // 当前接口超时等待时间
        int curTimeout = oldTimeout;
        boolean flag = false;
        ArrayList<PgInfo.Pg> pglist = new ArrayList<>();
        for (int i = 0; i < tryTimes; ++i) {
            if (flag)
                break;
            try {
                for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryAllPgInfo(cephip).entrySet()) {
                    if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                        PgInfo pgInfo = (PgInfo) entry.getValue().getData();
                        pglist = pgInfo.getPrimaryPgView();
                        flag = true;
                    } else if (entry.getValue().getStatusCode() == StatusCode.EXEC_COMMAND_FAILED) {
                        // 处理线程执行中断的情况 -> 休眠，等待下一次尝试
                        sleep(oldTimeout);
                    } else if (entry.getValue().getStatusCode() == StatusCode.EXEC_COMMAND_TIMEOUT) {
                        // 处理请求超时的情况 -> 增长至 increaseFactor * curTimeout
                        curTimeout = (int) Math.ceil(curTimeout * increaseFactor);
                        GlobalCacheSDK.setCommandTimeout(RegisterExecutor.QUERY_ALL_PG_INFO, curTimeout);
                    }
                }
            } catch (GlobalCacheSDKException | InterruptedException e) {
                System.out.println("接口调用失败");
                e.printStackTrace();
            }
        }
        try {
            GlobalCacheSDK.setCommandTimeout(RegisterExecutor.QUERY_ALL_PG_INFO, oldTimeout);
        } catch (GlobalCacheSDKException e) {
            System.out.println("设置执行时间失败");
            e.printStackTrace();
        }
        //根据nodeId将数据分类 封装 存入数据库
        for (int i = 0; i < hosts.size(); i++) {
            ArrayList<Pg> apgInfo = new ArrayList<>();
            int curid = ipmap.get(hosts.get(i));
            //按照nodeId分类
            for (int j = 0; j < pglist.size(); ++j) {
                PgInfo.Pg pg = pglist.get(j);
                if (pg.getMasterNode() == curid) {
                    Pg curpg = new Pg();
                    curpg.setPgId(pg.getPgId());
                    curpg.setBv(pg.getBv());
                    curpg.setMasterNode(pg.getMasterNode());
                    curpg.setMasterDisk(pg.getMasterDisk());
                    curpg.setCopyNum(pg.getCopyNum());
                    curpg.setState(pg.getState());
                    curpg.setNodeId(pg.getMasterNode());
                    curpg.setDiskId(pg.getMasterDisk());
                    ArrayList<PgInfo.PgCopyInfo> pgcopyinfolist = pg.getCopyInfos();
                    ArrayList<Pg.CopyInfo> copylist = new ArrayList<>();
                    for (PgInfo.PgCopyInfo ci : pgcopyinfolist) {
                        Pg.CopyInfo copyInfo = new Pg.CopyInfo();
                        copyInfo.setNodeId(ci.getNodeId());
                        copyInfo.setDiskId(ci.getDiskId());
                        copyInfo.setCopyState(ci.getState());
                        copylist.add(copyInfo);
                    }
                    curpg.setCopyInfos(copylist);
                    apgInfo.add(curpg);
                }
            }
            //封装
            PgList pgList = new PgList();
            pgList.setPgArrayList(apgInfo);
            String id = ipmap.get(hosts.get(i)) + "1" + time;
            pgList.setId(Long.parseLong(id));
            pgList.setTime(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
            //保存
            pgListData.savePgList(pgList);
        }

    }
}


