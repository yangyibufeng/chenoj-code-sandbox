package com.yybf.chenojcodesandbox.model;

import lombok.Data;

/**
 * 用来传递远程执行信息
 * @author yangyibufeng
 * @date 2024/2/19
 */
@Data
public class ExecuteMessage {
    /**
     * 用来传递控制台执行程序的返回值
     * 用Integer是为了防止使用int时为空的时候值会变成0，与程序正常执行的结果一样
     */
    private Integer exitValue;

    private String message;

    private String errorMessage;

    //表示程序执行时间
    private Long time;
}