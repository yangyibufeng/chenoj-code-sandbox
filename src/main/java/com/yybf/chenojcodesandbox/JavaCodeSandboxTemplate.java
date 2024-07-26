package com.yybf.chenojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yybf.chenojcodesandbox.model.ExecuteCodeRequest;
import com.yybf.chenojcodesandbox.model.ExecuteCodeResponse;
import com.yybf.chenojcodesandbox.model.ExecuteMessage;
import com.yybf.chenojcodesandbox.model.JudgeInfo;
import com.yybf.chenojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author yangyibufeng
 * @date 2024/7/22
 * 一个抽象出来的代码沙箱的模板方法类
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {


    public static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    public static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    // 存放用户代码的目录
    public static String USER_CODE_PARENT_PATH = "";

    public static final long TIME_OUT = 5000L;


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        // 创建原生的系统级别的安全管理器
//        System.setSecurityManager(new DefaultSecurityManager());
        System.out.println("使用了模板方法");

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        /* 1. 获取用户代码，将其保存为文件 */
        File userCodeFile = saveCodeToFile(code);


        /* 2. 将获取到的文件进行编译 */
        ExecuteMessage compiledFileExecuteMessage = compileFile(userCodeFile);
        System.out.println(compiledFileExecuteMessage);


        /* 3.执行代码，得到输出结果 */
        List<ExecuteMessage> executeMessageList = runFile(inputList);

        /* 4.整理收集输出结果 */
        ExecuteCodeResponse executeCodeResponse = getOutputResponse(executeMessageList);

        /* 5. 文件清理*/
        Boolean deleteFile = deleteFile(userCodeFile);
        if (!deleteFile) {
            log.error("文件清理异常，文件路径={}", userCodeFile.getAbsolutePath());
        }

        /* 6.错误处理，提升健壮性*/

        return executeCodeResponse;
    }

    /**
     * @param code: 用户代码
     * @return java.io.File:
     * @description 1. 获取用户代码，将其保存为文件
     * @author yangyibufeng
     * @date 2024/3/1 17:36
     */
    public File saveCodeToFile(String code) {
        // 获取到项目的根目录
        String userDir = System.getProperty("user.dir");
        // 拼接出存放临时代码的目录名称，
        // File.separator -- 是为了兼容不同的系统，当win是\\；当为linux是/
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断目录是否存在，不存在就创建这个目录
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        // 存放用户代码的目录（将每个用户代码都新建一个文件夹单独存放）
        USER_CODE_PARENT_PATH = globalCodePathName + File.separator + UUID.randomUUID();
        // 用户代码的真实路径
        String userCodePath = USER_CODE_PARENT_PATH + File.separator + GLOBAL_JAVA_CLASS_NAME;
        // 将用户代码写入文件
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * @param userCodeFile: 用户代码文件
     * @return com.yybf.chenojcodesandbox.model.ExecuteMessage:
     * @description 2. 将获取到的文件进行编译
     * @author yangyibufeng
     * @date 2024/3/1 17:42
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        // format是生成一个格式化的string字符串，getAbsolutePath()是获取文件的绝对路径
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            // 这个方法实质性cmd命令，返回结果是一个进程process
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
//            System.out.println(executeMessage);
//            log.info("模板方法中的executeMessage：{}",executeMessage);
            if (executeMessage.getExitValue() != 0) {
                throw new RuntimeException("编译错误");
            }
            return executeMessage;
        } catch (Exception e) {
//            return getResponse(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @param inputList: 输入的测试样例列表
     * @return java.util.List<com.yybf.chenojcodesandbox.model.ExecuteMessage>:
     * @description 3.执行代码，得到输出结果列表
     * @author yangyibufeng
     * @date 2024/3/1 17:53
     */
    public List<ExecuteMessage> runFile(List<String> inputList) {
        // 存储执行测试的结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();

        for (String inputArgs : inputList) {
            // 循环获取每一个输入测试用例
            // 第一个%s是需要运行文件所在目录 第二个%s是输入用例
            // -Xmx256m -- 指定JVM的空间为256M
            // -Dfile.encoding=UTF-8 指定编码为UTF-8
            // %s -Djava.security.manager=%s 这个表示在指定的程序中添加指定的权限校验
//            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeParentPath, SECURITY_MANAGER_PATH, SECURITY_MANAGER_CLASS_NAME, inputArgs);
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", USER_CODE_PARENT_PATH, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);

                // 开启一个子线程，来防止执行程序的线程超时

                new Thread(() -> {
                    try {
                        System.out.println("-----------------");
                        Thread.sleep(TIME_OUT);
                        if (runProcess.isAlive()) {
                            System.out.println("超时了，头给你炫掉！");
                        }
                        runProcess.destroy();
                    } catch (Exception e) {
                        System.out.println(getResponse(e));
                    }
                }).start();

                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                System.out.println("程序执行时间：" + executeMessage.getTime());

                /*
                通过交互式将用例传递给程序
                ExecuteMessage executeMessage = ProcessUtils.runInteractProcessAndGetMessage(runProcess,"运行",inputArgs);
                 */
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
//                return getResponse(e);
                throw new RuntimeException("运行代码错误", e);
            }
        }
        return executeMessageList;
    }

    /**
     * @param executeMessageList: 每个测试用例的输出结果
     * @return com.yybf.chenojcodesandbox.model.ExecuteCodeResponse:
     * @description 4.整理收集输出结果
     * @author yangyibufeng
     * @date 2024/3/1 18:00
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();

        List<String> outputList = new ArrayList<>();
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            // 如果有错误信息 -- 表示运行时出现错误
            if (StrUtil.isNotBlank(errorMessage)) {
                // 将返回信息设置为报错信息，并且将状态设置为3 表示运行时出现错误
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(time, maxTime);
            }
        }
        // 正常运行
        if (executeCodeResponse.getStatus() == null) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
//        judgeInfo.setMemory();
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);

        return executeCodeResponse;
    }

    /**
     * @param userCodeFile: 用户代码文件
     * @return java.lang.Boolean:
     * @description 5. 文件清理
     * @author yangyibufeng
     * @date 2024/3/1 18:06
     */
    public Boolean deleteFile(File userCodeFile) {
        if (userCodeFile.exists()) {
            boolean del = FileUtil.del(USER_CODE_PARENT_PATH);
            System.out.println("文件夹：" + USER_CODE_PARENT_PATH + "\n清理" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }


    /**
     * @param e:
     * @return com.yybf.chenojcodesandbox.model.ExecuteCodeResponse:
     * @description 6.获取错误响应
     * @author yangyibufeng
     * @date 2024/2/20 12:01
     */
    private ExecuteCodeResponse getResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 2表示代码沙箱出现的错误，例如编译错误等
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;

    }
}