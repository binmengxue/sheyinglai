package com.sheyinglai.common;

import lombok.Getter;

/** 业务异常：抛出后由全局异常处理器转成 Result */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String msg) {
        this(400, msg);
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
