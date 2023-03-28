package com.hw.hwbackend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ErrorHandler;

/**
 * 异步定时任务错误处理
 *
 * @author huan.fu 2021/7/8 - 下午2:39
 */
@Slf4j
public class SchedulerErrorHandler implements ErrorHandler {

    @Override
    public void handleError(Throwable throwable) {
        log.error("异步定时任务出现问题", throwable);
    }
}
