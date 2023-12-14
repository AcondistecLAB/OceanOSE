/**
 * Copyright (c) Huawei Technologies Co., Ltd. 2022-2022. All rights reserved.
 */

package com.vmware.osis.huawei.util;

public class ResultBuilder {
    private int code;

    private String msg;

    private Object data;

    private ResultBuilder() {
    }

    /**
     * success
     *
     * @param data data
     * @return ResultBuilder
     */
    public static ResultBuilder success(Object data) {
        return success(data, 200);
    }

    /**
     * success
     *
     * @param data data
     * @return ResultBuilder
     */
    public static ResultBuilder success(Object data, int code) {
        ResultBuilder resultBuilder = new ResultBuilder();
        resultBuilder.code = code;
        resultBuilder.msg = "";
        resultBuilder.data = data;
        return resultBuilder;
    }

    /**
     * failure
     *
     * @param message message
     * @param code code
     * @return ResultBuilder
     */
    public static ResultBuilder failure(String message, int code) {
        ResultBuilder resultBuilder = new ResultBuilder();
        resultBuilder.code = code;
        resultBuilder.msg = message;
        resultBuilder.data = null;
        return resultBuilder;
    }

    /**
     * custom
     *
     * @return ResultBuilder
     */
    public static ResultBuilder custom() {
        ResultBuilder resultBuilder = new ResultBuilder();
        return resultBuilder;
    }

    /**
     * code
     *
     * @param code code
     * @return ResultBuilder
     */
    public ResultBuilder code(int code) {
        this.code = code;
        return this;
    }

    /**
     * msg
     *
     * @param msg msg
     * @return ResultBuilder
     */
    public ResultBuilder msg(String msg) {
        this.msg = msg;
        return this;
    }

    /**
     * data
     *
     * @param data data
     * @return ResultBuilder
     */
    public ResultBuilder data(Object data) {
        this.data = data;
        return this;
    }

    /**
     * build
     *
     * @return Result
     */
    public Result<String> build() {
        return new Result(code, msg, data);
    }
}
