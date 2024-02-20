package com.yybf.chenojcodesandbox.security;

import java.io.File;
import java.security.Permission;

/**
 * @author yangyibufeng
 * 自定义的安全管理器 -- 禁止所有权限
 * @date 2024/2/20
 */
public class MySecurityManager extends SecurityManager {

    @Override
    public void checkPermission(Permission perm) {
        super.checkPermission(perm);
    }

    // 检测程序是否被允许执行cmd命令
    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("权限异常：" + cmd);
    }

    // 检测程序是否被允许读其他文件
    @Override
    public void checkRead(String file) {
        throw new SecurityException("权限异常：" + file);
    }

    // 检测程序是否被允许写其他文件
    @Override
    public void checkWrite(String file) {
        throw new SecurityException("权限异常：" + file);
    }

    // 检测程序是否被允许删除其他文件
    @Override
    public void checkDelete(String file) {
        throw new SecurityException("权限异常：" + file);
    }

    // 检测程序是否被允许连接网络
    @Override
    public void checkConnect(String host, int port) {
        throw new SecurityException("权限异常：" + host + ":" + port);
    }
}