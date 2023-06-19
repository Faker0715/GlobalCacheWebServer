package com.hw.hwbackend.config;

import com.hw.hwbackend.Interceptor.LoginInterceptor;
import com.hw.hwbackend.Interceptor.UserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 第二层拦截器 拦截非在线用户访问接口
        registry.addInterceptor(new LoginInterceptor(stringRedisTemplate))
                .excludePathPatterns(
                        "/getRegister",
                        "/getLoginPassword"
                ).order(1);

        // 第一层拦截 刷新用户token
        registry.addInterceptor(new UserInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
    }
}
