package com.hw.hwbackend.dto;

import lombok.Data;
// Websocket 对象
@Data
public class WebsocketDTO {
    private String url;
    private String chaneelId;
    private Params params;

    private String token;
    @Data
    public static class Params{
        private int nodeId = -1;
        private int diskId = -1;
        private String token;

        public Params() {

        }
    }
}
