package com.yybf.chenojcodesandbox.security;

import java.security.Permission;

/**
 * @author yangyibufeng
 * 所有权限都禁止的安全管理器
 * @date 2024/2/20
 */
public class DenySecurityManager extends SecurityManager {
    /**
     * @param perm:
     * @return void:
     * @author yangyibufeng
     * @description 检查所有的权限
     * @date 2024/2/20 21:38
     */
    @Override
    public void checkPermission(Permission perm) {
        throw new SecurityException("权限不足：" + perm.toString());
    }
}