package com.hw.hwbackend.exception;

import com.github.lazyboyl.websocket.exception.WebsocketGlobalException;

import java.util.HashMap;
import java.util.Map;

// websocket异常类 实现错误返回
public class WebsocketGlobalExceptionImpl implements WebsocketGlobalException {

    @Override
    public int level() {
        return 0;
    }

    @Override
    public Object errorHandler(Exception e) {
        e.printStackTrace(System.out);
        Map<String,Object> r = new HashMap<>();
        r.put("code",404);
        r.put("msg","失败了");
        return r;
    }
}
