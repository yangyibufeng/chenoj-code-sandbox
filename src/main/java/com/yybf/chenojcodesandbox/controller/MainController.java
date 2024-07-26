package com.yybf.chenojcodesandbox.controller;

import com.yybf.chenojcodesandbox.JavaDockerCodeSandbox;
import com.yybf.chenojcodesandbox.model.ExecuteCodeRequest;
import com.yybf.chenojcodesandbox.model.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author yangyibufeng
 * @date 2024/2/18
 */
@RestController("/")
@Slf4j
public class MainController {
    // 定义一个鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = "secretKey";

    @Resource
    private JavaDockerCodeSandbox javaDockerCodeSandbox;

    @GetMapping("/health")
    public String healthCheck(){
        return "ok";
    }

    @PostMapping("/fuck")
    public String postHealthCheck(){return "Fuck ok!";}

    /**
     * @param executeCodeRequest: 请求参数
     * @return com.yybf.chenojcodesandbox.model.ExecuteCodeResponse:
     * @author yangyibufeng
     * @description 执行代码
     * @date 2024/3/1 19:25
     */
    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest,
                                           HttpServletRequest request, HttpServletResponse response) {
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if(!AUTH_REQUEST_SECRET.equals(authHeader)){
            response.setStatus(403);
            return new ExecuteCodeResponse().builder().message("你没权限").build();
        }

        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }

        log.info("开始执行判题，输入的Request={}",executeCodeRequest);
        return javaDockerCodeSandbox.executeCode(executeCodeRequest);
    }
}