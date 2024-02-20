package com.yybf.chenojcodesandbox.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yangyibufeng
 * @date 2024/2/18
 */
@RestController("/")
public class MainController {

    @GetMapping("/health")
    public String healthCheck(){
        return "ok";
    }
}