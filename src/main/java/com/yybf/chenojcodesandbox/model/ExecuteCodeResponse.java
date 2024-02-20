package com.yybf.chenojcodesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author yangyibufeng
 * @date 2024/2/6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeResponse {

    private List<String> outputList;

    // 接口信息
    private String message;

    // 程序执行状态
    private Integer status;

    private JudgeInfo judgeInfo;
}