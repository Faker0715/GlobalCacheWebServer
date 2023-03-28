package com.hw.hwbackend.controller;

import com.github.lazyboyl.websocket.annotation.WebSocketController;
import com.github.lazyboyl.websocket.annotation.WebSocketRequestMapping;
import com.github.lazyboyl.websocket.annotation.WebSocketRequestParam;
import com.hw.hwbackend.service.AbstractService;
import com.hw.hwbackend.service.CpuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@WebSocketController
@RestController
public class CpuController {
    @Autowired
    private AbstractService abstractService;
    @Autowired CpuService cpuService;
    @GetMapping("/getCpuData")
    @WebSocketRequestMapping("/getCpuData")
    public void getCpuData(@WebSocketRequestParam(name = "nodeId") int nodeId) throws IOException {
//        abstractService.getCpuData();
    }

}
