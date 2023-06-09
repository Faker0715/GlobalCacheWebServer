package com.hw.hwbackend.controller;

import com.github.lazyboyl.websocket.annotation.WebSocketController;
import com.github.lazyboyl.websocket.annotation.WebSocketRequestMapping;
import com.hw.hwbackend.service.AbstractService;
import com.hw.hwbackend.service.CpuCalenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@WebSocketController
@RestController
public class CpuCalenderController {
    @Autowired
    private AbstractService abstractService;

    @GetMapping("/getCpuCalender")
    @WebSocketRequestMapping("/getCpuCalender")
    public void getCpuCalender() throws IOException {
       abstractService.getCpuCalender();
    }

}