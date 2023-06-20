package com.hw.hwbackend.securityservice.impl;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hw.hwbackend.entity.*;
import com.hw.hwbackend.mapper.RegMapper;
import com.hw.hwbackend.mapper.UserMapper;
import com.hw.hwbackend.securityservice.LoginServcie;
import com.hw.hwbackend.util.JwtUtil;
import com.hw.hwbackend.util.ResponseResult;
import com.hw.hwbackend.util.UserHolder;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginServcie {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;



    @Autowired
    private RegMapper regMapper;


    @Autowired
    private UserMapper userMapper;

    @Override
    public ResponseResult login(User user) {
        //AuthenticationManager authenticate进行用户认证

        System.out.println("auth start!");

        String encode = passwordEncoder.encode(user.getPassword());

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user.getUserName(), user.getPassword());

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        System.out.println(queryWrapper+"qurry");
        queryWrapper.eq(User::getUserName,user.getUserName());


        User user1 = regMapper.getUserbyName(user.getUserName());
//        User user1 = UserMapper.selectOne(queryWrapper);
        System.out.println("lalala"+user1);


        Map<String, String> map = new HashMap<>();
        map.put("token","");
        //如果没有查询到用户就抛出异常
        if(Objects.isNull(user1)){
            System.out.println("kong");
            //throw new RuntimeException("用户名或者密码错误");
            return new ResponseResult(false,map,1,"用户名或密码错误");
        }
        if(!passwordEncoder.matches(user.getPassword(), user1.getPassword())) {
            return new ResponseResult(false,map,1,"用户名或密码错误");
        }

        Authentication authenticate = authenticationManager.authenticate(authenticationToken);


        System.out.println("auth end!");
        //如果认证通过了，使用username生成一个jwt jwt存入ResponseResult返回
        LoginUser loginUser = (LoginUser) authenticate.getPrincipal();
        String username = loginUser.getUser().getUserName().toString();
        LoginUser redisUser = JSON.parseObject(stringRedisTemplate.opsForValue().get("jwt:" + username), LoginUser.class);
        String token = "";
        if(redisUser != null && !(redisUser.getToken().equals(""))){
            token = redisUser.getToken();
        }
        if (token == "") {
            token = JwtUtil.createJWT(username);
        }

        loginUser.setToken(token);
        map.put("token", token);
        loginUser.setTime(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli());
        stringRedisTemplate.opsForValue().set("jwt:" + username, JSONUtil.toJsonStr(loginUser), 30 * 60, TimeUnit.SECONDS);
        int isSpuer = loginUser.getUser().getUserType();
//        System.out.println("issuper: " + isSpuer);
        UserHolder userHolder = UserHolder.getInstance();
        userHolder.addAuto(token, new AutoList());
        return new ResponseResult(true, map, isSpuer,"登录成功");
    }

    @Override
    public ResponseResult logout(HttpServletRequest request) {
        //删除redis中的值
        String token = request.getHeader("token");
        String userid = "";
        try {
            Claims claims = JwtUtil.parseJWT(token);
            userid = claims.getSubject();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("token非法");
        }

        UserHolder userHolder = UserHolder.getInstance();
        userHolder.deleteAuto(token);
        stringRedisTemplate.delete("jwt:" + userid);
        return new ResponseResult(200, "注销成功");
    }

    @Override
    public ResponseResult loginToken(HttpServletRequest request) {
        String token = request.getHeader("token");
        String userid = "";
        try {
            Claims claims = JwtUtil.parseJWT(token);
            userid = claims.getSubject();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("token非法");
        }
        LoginUser loginUser = JSON.parseObject(stringRedisTemplate.opsForValue().get("jwt:" + userid), LoginUser.class);

        Map<String,Object> map = new HashMap<>();
        map.put("phonenumber", loginUser.getUser().getPhonenumber());
        map.put("userName", loginUser.getUser().getUserName());
        map.put("password", loginUser.getUser().getPassword());
        map.put("token", token);
        map.put("isSuperUser", loginUser.getUser().getUserType());
        map.put("isFinished", regMapper.getfinished() == 1?true:false);

        return new ResponseResult<Map<String,Object>>(map);
    }

}
