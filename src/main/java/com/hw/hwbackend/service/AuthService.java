package com.hw.hwbackend.service;

import com.hw.hwbackend.util.UserHolder;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class AuthService {

    private static Logger log = LoggerFactory.getLogger(AuthService.class);

    ArrayList<String> infolist = new ArrayList<>();

    /**
     * 功能描述： 实现模拟鉴权
     *
     * @param url 当前响应的地址
     * @return
     */
    public Boolean authUrl(String url) {
        // 获取当前权限
        UserHolder userHolder = UserHolder.getInstance();
        if (userHolder.getUrlarray().contains(url)) {
            return true;
        }
        return false;
    }

}


