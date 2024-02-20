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
@NoArgsConstructor // 给类生成一个无参的构造方法
@AllArgsConstructor // 给类生成一个包含所有参数的构造方法
public class ExecuteCodeRequest {

    private List<String> inputList;

    private String code;

    private String language;
}