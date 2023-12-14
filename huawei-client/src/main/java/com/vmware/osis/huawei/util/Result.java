/**
 * Copyright (c) Huawei Technologies Co., Ltd. 2022-2022. All rights reserved.
 */

package com.vmware.osis.huawei.util;

import java.io.Serializable;

public class Result<T> implements Serializable {

    /**
     * 响应码
     */
    private int code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 无参构造方法
     */
    public Result() {
    }

    /**
     * 全参构造方法
     *
     * @param code code
     * @param msg msg
     * @param data data
     */
    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean is2xxSuccess() {
        return this.code / 100 == 2;
    }

    @Override
    public String toString() {
        return "{" + "code:" + code + ", msg:'" + msg + '\'' + ", data:" + data + '}';
    }
}
