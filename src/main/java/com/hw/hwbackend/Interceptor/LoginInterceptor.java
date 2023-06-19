package com.hw.hwbackend.Interceptor;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.hw.hwbackend.entity.LoginUser;
import com.hw.hwbackend.util.JwtUtil;
import com.hw.hwbackend.util.UserContext;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.判断是否需要拦截（ThreadLocal中是否有用户）

        String token = request.getHeader("token");
        if (StrUtil.isBlank(token)) {
            return true;
        }
        String userid = "";
        try {
            Claims claims = JwtUtil.parseJWT(token);
            userid = claims.getSubject();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("token非法");
        }
        // 2.基于TOKEN获取redis中的用户
        String key  =  "jwt:" + userid;
        LoginUser loginUser =  JSON.parseObject(stringRedisTemplate.opsForValue().get("jwt:"+userid),LoginUser.class);
        // 3.判断用户是否存在
        if (loginUser == null) {
            return true;
        }

        if (UserContext.getCurrentUser() == null) {
            System.out.println("401401401401401");
            // 没有，需要拦截，设置状态码
            response.setStatus(401);
            // 拦截
            return false;
        }
        // 有用户，则放行
        return true;
    }

}
