package com.tom.shop.dto;

import lombok.Data;

@Data
public class SeckillResult {
    private boolean success;
    private String message;
    private Object data;

    public static SeckillResult ok(Object data) {
        SeckillResult result = new SeckillResult();
        result.setSuccess(true);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    public static SeckillResult error(String message) {
        SeckillResult result = new SeckillResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }
}
