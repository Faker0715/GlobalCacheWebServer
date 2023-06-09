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
        System.out.println(e);
        Map<String,Object> r = new HashMap<>();
//        System.out.println(e);
        r.put("code",404);
        r.put("msg","失败了");
        return r;
    }
}
