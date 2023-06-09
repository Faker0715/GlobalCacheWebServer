package com.hw.hwbackend.security;

import com.github.lazyboyl.websocket.listenter.WebSocketHandlerListenter;
import com.hw.hwbackend.dto.WebsocketDTO;
import com.hw.hwbackend.util.UserHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;
public class WebSocketHandlerListenterImpl implements WebSocketHandlerListenter {

    private static WebSocketHandlerListenterImpl webSocketHandlerListenter;

    public static WebSocketHandlerListenterImpl getInstance() {
        if (webSocketHandlerListenter== null) {
            webSocketHandlerListenter = new WebSocketHandlerListenterImpl();
        }
        return webSocketHandlerListenter;
    }

    // socketid -> channel
    public ConcurrentHashMap<String, Channel> chanelIdMap = new ConcurrentHashMap<>();
    // socketid -> websocketdto
    public ConcurrentHashMap<String, WebsocketDTO> WebsocketMap = new ConcurrentHashMap<>();

    @Override
    public int level() {
        return 0;
    }

    /**
     * 功能描述： 当浏览器端的通道关闭的时候的响应处理方法
     *
     * @param ctx 当前的通道对象
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("current end channel: " + ctx.channel().id().asLongText());
        WebSocketHandlerListenterImpl.getInstance().WebsocketMap.remove(ctx.channel().id().asLongText());
        WebSocketHandlerListenterImpl.getInstance().chanelIdMap.remove(ctx.channel().id().asLongText());
    }

    @Override
    public void handleShake(ChannelHandlerContext ctx) {
        System.out.println("current start channel：" + ctx.channel().id().asLongText());
        WebSocketHandlerListenterImpl.getInstance().chanelIdMap.put(ctx.channel().id().asLongText(), ctx.channel());
        System.out.println("websocketchannelidmap: " + webSocketHandlerListenter + " " + webSocketHandlerListenter.chanelIdMap.size());
    }
}
