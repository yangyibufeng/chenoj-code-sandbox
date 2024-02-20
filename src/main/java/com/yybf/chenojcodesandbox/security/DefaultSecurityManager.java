package com.yybf.chenojcodesandbox.security;

import org.springframework.boot.ansi.AnsiOutput;

import java.security.Permission;

/**
 * @author yangyibufeng
 * 默认的安全管理器
 * @date 2024/2/20
 */
public class DefaultSecurityManager extends SecurityManager {
    /**
     * @param perm:
     * @return void:
     * @author yangyibufeng
     * @description 检查所有的权限
     * @date 2024/2/20 21:38
     */
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认不做任何限制");
        System.out.println(perm);
        super.checkPermission(perm);
    }
}