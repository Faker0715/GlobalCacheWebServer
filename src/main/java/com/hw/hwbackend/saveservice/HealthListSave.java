package com.hw.hwbackend.saveservice;

import com.hw.globalcachesdk.GlobalCacheSDK;
import com.hw.globalcachesdk.StatusCode;
import com.hw.globalcachesdk.entity.ClusterAlarmInfo;
import com.hw.globalcachesdk.entity.ClusterStatusInfo;
import com.hw.globalcachesdk.entity.DiskInfo;
import com.hw.globalcachesdk.entity.PgInfo;
import com.hw.globalcachesdk.exception.GlobalCacheSDKException;
import com.hw.globalcachesdk.executor.CommandExecuteResult;
import com.hw.globalcachesdk.executor.RegisterExecutor;
import com.hw.hwbackend.dataservice.HealthListData;
import com.hw.hwbackend.entity.HealthList;
import com.hw.hwbackend.util.UserHolder;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
//从集群获取节点健康数据 存入数据库

import static java.lang.Thread.sleep;

@Service
public class HealthListSave {

    @Autowired
    private HealthListData healthListData;

    public void HealthListSchedule() {
        ArrayList<Tuple3<String,String,String>> infolist = new ArrayList<>();
        //根据查询到的日志信息 判断节点的健康状态
        String cephip = UserHolder.getInstance().getCeph1();

        infolist.add(Tuple.of("ERROR", "NODE", "temporarily faulty and cannot provide servicestemporarily."));
        infolist.add(Tuple.of("ERROR", "NODE", "permanently faulty and removed from the cluster"));
        infolist.add(Tuple.of("ERROR", "DISK", "disk is faulty."));
        infolist.add(Tuple.of("WARN", "DISK", "disk capacity of the ceph pool reaches 80%."));
        infolist.add(Tuple.of("ERROR", "DISK", "disk capacity of the ceph pool is insufficient."));
        infolist.add(Tuple.of("WARN", "DISK", "disk capacity of the log system reaches 80%."));
        infolist.add(Tuple.of("ERROR", "DISK", "disk capacity of the log system is insufficient."));
        infolist.add(Tuple.of("ERROR", "NETWORK", "the network connection is abnormal."));
        infolist.add(Tuple.of("WARN", "LOGSYSTEM", "failed to open the log file."));
        infolist.add(Tuple.of("WARN", "LOGSYSTEM", "failed to write the log file."));
        infolist.add(Tuple.of("WARN", "LOGSYSTEM", "failed to rename the log file."));
        infolist.add(Tuple.of("WARN", "LOGSYSTEM", "failed to remove the log file."));
        infolist.add(Tuple.of("ERROR", "DISK", "the disk has fault state pt."));


        // 失败尝试次数
        int tryTimes = 5;
        int oldTimeout = 0;
        try {
            // 记录当前接口默认超时等待时间
            oldTimeout = GlobalCacheSDK.getCommandTimeout(RegisterExecutor.QUERY_CLUSTER_STATUS_INFO);
        } catch (GlobalCacheSDKException e) {
            System.out.println("获取执行时间失败");
            e.printStackTrace();
        }
        // 时长增长因子
        double increaseFactor = 1.5f;
        // 当前接口超时等待时间
        int curTimeout = oldTimeout;
        boolean flag = false;
        HealthList healthList = new HealthList();
        for (int i = 0; i < tryTimes; ++i) {
            if (flag)
                break;
            try {
                for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryClusterStatusInfo(cephip).entrySet()) {
                    if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                        ClusterStatusInfo status = (ClusterStatusInfo) entry.getValue().getData();
                        if (status.getClusterStatus() == ClusterStatusInfo.ClusterStatus.CLUSTER_STATE_OK) {
                            healthList.setClusterState("true");
                        } else {
                            healthList.setClusterState("false");
                        }
                        flag = true;
                    } else if (entry.getValue().getStatusCode() == StatusCode.EXEC_COMMAND_FAILED) {
                        // 处理线程执行中断的情况 -> 休眠，等待下一次尝试
                        sleep(oldTimeout);
                    } else if (entry.getValue().getStatusCode() == StatusCode.EXEC_COMMAND_TIMEOUT) {
                        // 处理请求超时的情况 -> 增长至 increaseFactor * curTimeout
                        curTimeout = (int) Math.ceil(curTimeout * increaseFactor);
                        GlobalCacheSDK.setCommandTimeout(RegisterExecutor.QUERY_CLUSTER_STATUS_INFO, curTimeout);
                    }
                }
            } catch (GlobalCacheSDKException | InterruptedException e) {
                System.out.println("接口调用失败");
                e.printStackTrace();
            }
        }
        // 最后需要设置回原来的默认值
        try {
            GlobalCacheSDK.setCommandTimeout(RegisterExecutor.QUERY_CLUSTER_STATUS_INFO, oldTimeout);
        } catch (GlobalCacheSDKException e) {
            System.out.println("设置执行时间失败");
            e.printStackTrace();
        }

        flag = false;
        try {
            // 记录当前接口默认超时等待时间
            oldTimeout = GlobalCacheSDK.getCommandTimeout(RegisterExecutor.QUERY_CLUSTER_ALARM_INFO);
        } catch (GlobalCacheSDKException e) {
            System.out.println("获取执行时间失败");
            e.printStackTrace();
        }
        ArrayList<HealthList.Health> healthArrayList = new ArrayList<>();

        for (int i = 0; i < tryTimes; ++i) {
            if (flag)
                break;
            try {
                for (Map.Entry<String, CommandExecuteResult> entry : GlobalCacheSDK.queryClusterAlarmInfo(cephip).entrySet()) {
                    if (entry.getValue().getStatusCode() == StatusCode.SUCCESS) {
                        ClusterAlarmInfo alarmInfo = (ClusterAlarmInfo) entry.getValue().getData();
                        ArrayList<ClusterAlarmInfo.AlarmInfo> alarmInfoArraylist = alarmInfo.getAlarmInfoList();
                        for (int k = 0; k < alarmInfoArraylist.size(); ++k) {
                            String log = alarmInfoArraylist.get(k).getLog();
                            HealthList.Health h = new HealthList.Health();
                            h.setAbnDetails(log);
                            for (int t = 0; t < infolist.size(); ++t) {
                                if (log.equals(infolist.get(t)._3)) {
                                    h.setAbnLevel(infolist.get(t)._1);
                                    h.setAbnType(infolist.get(t)._2);
                                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
                                    String date = df.format(alarmInfoArraylist.get(k).getTime());// new Date()为获取当前系统时间，也可使用当前时间戳
                                    h.setAbnTime(date);
                                    healthArrayList.add(h);
                                    break;
                                }
                            }
                        }
                        flag = true;
                    } else if (entry.getValue().getStatusCode() == StatusCode.EXEC_COMMAND_FAILED) {
                        // 处理线程执行中断的情况 -> 休眠，等待下一次尝试
                        sleep(oldTimeout);
                    } else if (entry.getValue().getStatusCode() == StatusCode.EXEC_COMMAND_TIMEOUT) {
                        // 处理请求超时的情况 -> 增长至 increaseFactor * curTimeout
                        curTimeout = (int) Math.ceil(curTimeout * increaseFactor);
                        GlobalCacheSDK.setCommandTimeout(RegisterExecutor.QUERY_CLUSTER_ALARM_INFO, curTimeout);
                    }
                }
            } catch (GlobalCacheSDKException | InterruptedException e) {
                System.out.println("接口调用失败");
                e.printStackTrace();
            }
        }
        try {
            GlobalCacheSDK.setCommandTimeout(RegisterExecutor.QUERY_CLUSTER_ALARM_INFO, oldTimeout);
        } catch (GlobalCacheSDKException e) {
            System.out.println("设置执行时间失败");
            e.printStackTrace();
        }
        //保存
        healthList.setHealthArrayList(healthArrayList);
        healthList.setId(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        healthListData.saveHealthList(healthList);

    }

}
