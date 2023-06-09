package com.hw.hwbackend.security;


import com.github.lazyboyl.websocket.security.WebsocketSecurity;
import com.github.lazyboyl.websocket.server.channel.entity.SocketRequest;
import com.github.lazyboyl.websocket.server.channel.entity.SocketResponse;
import com.github.lazyboyl.websocket.util.JsonUtils;
import com.hw.hwbackend.dto.WebsocketDTO;
import com.hw.hwbackend.service.AuthService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.beans.factory.annotation.Autowired;
import com.hw.hwbackend.security.WebSocketHandlerListenterImpl;
public class WebsocketSecurityImpl implements WebsocketSecurity {

    @Autowired
    private AuthService authService;
    // 设置拦截等级
    @Override
    public int level() {
        return 10;
    }

    @Override
    public Boolean authentication(ChannelHandlerContext ctx, SocketRequest socketRequest) {
        // 先判断url是否是websocket的url
        Boolean isPass = authService.authUrl(socketRequest.getUrl());
        WebSocketHandlerListenterImpl webSocketHandlerListenterImpl = WebSocketHandlerListenterImpl.getInstance();
//        System.out.println(socketRequest.getUrl());
        // 如果不通过
        if(!isPass){
            // 先给前端发一条消息
            ctx.channel().writeAndFlush(new TextWebSocketFrame(JsonUtils.objToJson(new SocketResponse(HttpResponseStatus.UNAUTHORIZED.code(), "授权不通过！"))));
            // 如果存在连接 那么主动删除对象

            if(webSocketHandlerListenterImpl.WebsocketMap.contains(ctx.channel().id().asLongText())){
               webSocketHandlerListenterImpl.WebsocketMap.remove(ctx.channel().id().asLongText());
            }
            // 主动断开连接
            ctx.channel().close();
        }else{
            // 保存新建对象
            WebsocketDTO websocketDTO = new WebsocketDTO();
            websocketDTO.setUrl(socketRequest.getUrl());
            websocketDTO.setChaneelId(socketRequest.getSocketId());
            WebsocketDTO.Params params = new WebsocketDTO.Params();
            if(socketRequest.getParams().get("nodeId") != null){
               params.setNodeId(Integer.parseInt(String.valueOf(socketRequest.getParams().get("nodeId"))));
            }
            if(socketRequest.getParams().get("diskId") != null){
                params.setNodeId(Integer.parseInt(String.valueOf(socketRequest.getParams().get("diskId"))));
            }
            if(socketRequest.getParams().get("token") != null){
                params.setToken(String.valueOf(socketRequest.getParams().get("token")));
            }
            websocketDTO.setParams(params);
            webSocketHandlerListenterImpl.WebsocketMap.put(ctx.channel().id().asLongText(),websocketDTO);
        }
        return isPass;
    }
}