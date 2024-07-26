package com.yybf.chenojcodesandbox;

import com.yybf.chenojcodesandbox.model.ExecuteCodeRequest;
import com.yybf.chenojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

/**
 * 通过模板复用来编写java原生实现代码沙箱的代码
 * @author yangyibufeng
 * @date 2024/7/22
 */
@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}