package com.hw.hwbackend.controller;

import com.github.lazyboyl.websocket.annotation.WebSocketController;
import com.github.lazyboyl.websocket.annotation.WebSocketRequestMapping;
import com.github.lazyboyl.websocket.annotation.WebSocketRequestParam;
import com.hw.hwbackend.service.AbstractService;
import com.hw.hwbackend.service.MemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@WebSocketController
@RestController
public class MemoryController {
    @Autowired
    private AbstractService abstractService;
    @Autowired
    private MemoryService memoryService;
    @GetMapping("/getMemoryData")
    @WebSocketRequestMapping("/getMemoryData")
    public void getMemoryData(@WebSocketRequestParam(name = "nodeId") int nodeId) throws IOException {
        abstractService.getMemoryData();
    }

}
