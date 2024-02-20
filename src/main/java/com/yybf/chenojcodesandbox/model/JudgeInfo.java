package com.yybf.chenojcodesandbox.model;

import lombok.Data;

/**
 * 判题信息
 *
 * @author yangyibufeng
 * @date 2024/1/28
 */
@Data
public class JudgeInfo {
    /**
     * @author yangyibufeng
     * @description 程序执行信息
     * @date 2024/1/28 23:23
     */
    private String message;
    /**
     * @author yangyibufeng
     * @description 消耗内存（KB）
     * @date 2024/1/28 23:23
     */
    private Long memory;
    /**
     * @author yangyibufeng
     * @description 消耗时间（ms）
     * @date 2024/1/28 23:23
     */
    private Long time;

}