package com.hw.hwbackend.filter;

import com.alibaba.fastjson.JSON;
import com.hw.hwbackend.entity.LoginUser;
import com.hw.hwbackend.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 获取token
        String token = request.getHeader("token");
        // 如果没有token 就放行
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        //解析token
        String userid;
        try {
            Claims claims = JwtUtil.parseJWT(token);
            userid = claims.getSubject();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("token过期或者非法");
        }

        //从redis中获取用户信息
        String redisKey = "jwt:" + userid;
        LoginUser loginUser =  JSON.parseObject(stringRedisTemplate.opsForValue().get(redisKey),LoginUser.class);
//        System.out.println(loginUser);
        if (Objects.isNull(loginUser)) {
            throw new RuntimeException("用户未登录");
        }
        //存入SecurityContextHolder
        //TODO 获取权限信息封装到Authentication中
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        //放行
        filterChain.doFilter(request, response);
    }
}
