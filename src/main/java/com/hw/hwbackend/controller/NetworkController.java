package com.hw.hwbackend.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.lazyboyl.websocket.annotation.WebSocketController;
import com.github.lazyboyl.websocket.annotation.WebSocketRequestMapping;
import com.github.lazyboyl.websocket.annotation.WebSocketRequestParam;
import com.hw.hwbackend.entity.Disk;
import com.hw.hwbackend.entity.Network;
import com.hw.hwbackend.service.AbstractService;
import com.hw.hwbackend.service.DiskService;
import com.hw.hwbackend.service.NetworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@WebSocketController
@RestController
public class NetworkController {
    @Autowired
    private AbstractService abstractService;
    @Autowired
    private NetworkService networkService;
    @GetMapping("/getNetData")
    @WebSocketRequestMapping("/getNetData")
    public void getNetData(@WebSocketRequestParam(name = "nodeId") int nodeId) throws IOException {
        abstractService.getNetData();
    }

}
