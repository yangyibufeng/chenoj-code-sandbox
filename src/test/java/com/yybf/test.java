package com.yybf;

import com.yybf.chenojcodesandbox.model.ExecuteCodeResponse;
import com.yybf.chenojcodesandbox.model.JudgeInfo;

import java.util.List;

/**
 * @author yangyibufeng
 * 用来测试一些情况
 * @date 2024/2/20
 */
public class test {
    public static void main(String[] args) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = executeCodeResponse.getOutputList();
        String message = executeCodeResponse.getMessage();
        Integer status = executeCodeResponse.getStatus();
        JudgeInfo judgeInfo = executeCodeResponse.getJudgeInfo();
        System.out.println(status);
    }

}