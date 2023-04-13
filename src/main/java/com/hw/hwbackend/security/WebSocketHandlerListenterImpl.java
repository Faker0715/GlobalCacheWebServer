package com.hw.hwbackend.security;

import com.github.lazyboyl.websocket.listenter.WebSocketHandlerListenter;
import com.hw.hwbackend.dto.WebsocketDTO;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketHandlerListenterImpl implements WebSocketHandlerListenter {
    // socketid -> channel
    public static ConcurrentHashMap<String, Channel> chanelIdMap = new ConcurrentHashMap<>();
    // socketid -> websocketdto
    public static ConcurrentHashMap<String, WebsocketDTO> WebsocketMap = new ConcurrentHashMap<>();

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
//        System.out.println("当前关闭的通道的id是：" + ctx.channel().id().asLongText());
//        webSocketCloseService.removeChannel(ctx.channel().id().asLongText());
        WebsocketMap.remove(ctx.channel().id().asLongText());
        chanelIdMap.remove(ctx.channel().id().asLongText());
    }

    @Override
    public void handleShake(ChannelHandlerContext ctx) {
//        System.out.println("当前开启的通道的id是：" + ctx.channel().id().asLongText());
        chanelIdMap.put(ctx.channel().id().asLongText(),ctx.channel());
    }
}
