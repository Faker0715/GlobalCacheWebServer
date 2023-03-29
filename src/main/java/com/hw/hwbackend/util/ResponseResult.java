package com.hw.hwbackend.util;

import java.io.Serializable;
import java.util.Map;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.MapSerializer;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.SerializeConfig;

public class ResponseResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 返回的错误码
     */
    private int code;
    private String message;
    private boolean vaild;
    private int isSuperUser;
    private String reason;

    /**
     * 返回的消息
     */
    private boolean isSuccessed;


    /**
     * 返回的数据
     */
    private T data;
    //logout
    public ResponseResult(int code,String message) {
        this.code = code;
        this.message = message;
    }
    public ResponseResult(T data,boolean isSuccessed,String reason) {
        this.data = data;
        this.isSuccessed = isSuccessed;
        this.reason = reason;
    }
    // loginPassword
    public ResponseResult(boolean vaild,T data,int isSuperUser,String reason) {
        this.vaild = vaild;
        this.data = data;
        this.isSuperUser = isSuperUser;
        this.reason = reason;
    }
    // logintoken
    public ResponseResult(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

//    /**
//     * 返回成功的结果（不带数据）
//     * @return
//     */
//    public static <T> ResponseResult<T> success() {
//        return new ResponseResult<T>(200, "操作成功");
//    }
//
//    /**
//     * 返回成功的结果（带数据）
//     * @param data 返回的数据
//     * @return
//     */
//    public static <T> ResponseResult<T> success(T data) {
//        return new ResponseResult<T>(200, "操作成功", data);
//    }
//
//    /**
//     * 返回失败的结果
//     * @param code 错误码
//     * @param message 错误信息
//     * @return
//     */
//    public static <T> ResponseResult<T> fail(int code, String message) {
//        return new ResponseResult<T>(code, message);
//    }

    /**
     * 将当前对象转化为JSON格式的字符串
     * @return
     */
    public String toJsonString() {
        // 添加fastjson序列化Map泛型参数支持
        SerializeConfig config = new SerializeConfig();
        config.put(Map.class, new MapSerializer());
        // 写入类型信息和处理Map中的null值
        return JSON.toJSONString(this, config, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteClassName);
    }

}
