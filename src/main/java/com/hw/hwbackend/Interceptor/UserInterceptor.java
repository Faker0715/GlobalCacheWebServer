package com.hw.hwbackend.Interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.hw.hwbackend.dto.UserDTO;
import com.hw.hwbackend.entity.LoginUser;
import com.hw.hwbackend.entity.User;
import com.hw.hwbackend.util.JwtUtil;
import com.hw.hwbackend.util.UserContext;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UserInterceptor implements HandlerInterceptor {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public UserInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("token");
        if (StrUtil.isBlank(token)) {
            return true;
        }
        String userid = "";
        try {
            Claims claims = JwtUtil.parseJWT(token);
            userid = claims.getSubject();
        } catch (Exception e) {
            System.out.println("token invalid");
            e.printStackTrace();
            throw new RuntimeException("token非法");
        }
        System.out.println("userid : " + userid);
        // 1.基于TOKEN获取redis中的用户
        String key  =  "jwt:" + userid;
        LoginUser loginUser =  JSON.parseObject(stringRedisTemplate.opsForValue().get("jwt:"+userid),LoginUser.class);
        // 2.判断用户是否存在
        if (loginUser == null) {
            System.out.println("user null");
            return true;
        }
        // 3.存在，保存用户信息到 ThreadLocal
        UserContext.setCurrentUser(loginUser.getUser());
        // 4.设置刷新时间
        stringRedisTemplate.opsForValue().set("jwt:" + loginUser.getUser().getUserName(), JSONUtil.toJsonStr(loginUser), 30 * 60, TimeUnit.SECONDS);
        // 5.刷新token有效期
        stringRedisTemplate.expire(key, 30*60, TimeUnit.MINUTES);
        // 6.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserContext.clear();
    }
}
