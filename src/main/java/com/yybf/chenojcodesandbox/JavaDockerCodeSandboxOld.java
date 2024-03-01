package com.yybf.chenojcodesandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.yybf.chenojcodesandbox.model.ExecuteCodeRequest;
import com.yybf.chenojcodesandbox.model.ExecuteCodeResponse;
import com.yybf.chenojcodesandbox.model.ExecuteMessage;
import com.yybf.chenojcodesandbox.model.JudgeInfo;
import com.yybf.chenojcodesandbox.utils.ProcessUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author yangyibufeng
 * @date 2024/2/18
 */
public class JavaDockerCodeSandboxOld implements CodeSandbox {

    public static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    public static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    public static final long TIME_OUT = 15000L;

    private static final String SECURITY_MANAGER_PATH = "D:\\resources\\idea_code\\YuPi\\ChenOJ\\chenoj-code-sandbox\\src\\main\\resources\\testCode\\security";

    private static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";

    public static final Boolean FIRST_INIT = true;


    public static void main(String[] args) {
        JavaDockerCodeSandboxOld javaNativeCodeSandbox = new JavaDockerCodeSandboxOld();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "114 514"));
//        executeCodeRequest.setInputList(Arrays.asList("1 2", "114 514","2 3","4 5","9 8"));
        // hutool工具类中的一个可以直接访问到resource目录中文件的工具类
//        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        String code = ResourceUtil.readStr("testCode/simpleComputeArgsHC/Main.java", StandardCharsets.UTF_8);

//        String code = ResourceUtil.readStr("testCode/unsafeCode/SleepError.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/unsafeCode/MemoryError.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/unsafeCode/RunFileError.java", StandardCharsets.UTF_8);
        /*
        通过交互式将用例传递给程序
        String normalCode = ResourceUtil.readStr("testCode/simpleCompute/Main.java", StandardCharsets.UTF_8);
         */
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        // 创建原生的系统级别的安全管理器
//        System.setSecurityManager(new DefaultSecurityManager());

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();


        // 获取到项目的根目录
        String userDir = System.getProperty("user.dir");
        // 拼接出存放临时代码的目录名称，
        // File.separator -- 是为了兼容不同的系统，当win是\\；当为linux是/
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断目录是否存在，不存在就创建这个目录
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        /* 1. 获取用户代码，将其保存为文件 */
        // 存放用户代码的目录（将每个用户代码都新建一个文件夹单独存放）
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        // 用户代码的真实路径
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        // 将用户代码写入文件
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        /* 2. 将获取到的文件进行编译 */
        // format是生成一个格式化的string字符串，getAbsolutePath()是获取文件的绝对路径
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            // 这个方法实质性cmd命令，返回结果是一个进程process
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
        } catch (Exception e) {
            return getResponse(e);
//            throw new RuntimeException(e);
        }

        /* 3.创建容器，把文件复制到容器内*/
        // 获取默认的Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        // 拉取镜像
        String image = "openjdk:8-alpine";
        if (FIRST_INIT) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像失败：" + e.getMessage());
                throw new RuntimeException(e);
            }
        }
        System.out.println("下载完成");

        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L); // 限制内存与硬盘之间的数据交换
//        hostConfig.withSecurityOpts(Arrays.asList("seccomp安全管理配置"));
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withReadonlyRootfs(true) // 限制用户不向root文件写数据
                .withNetworkDisabled(true) // 限制容器的网络连接
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();
        System.out.println(createContainerResponse);
        // 获取容器id
        String containerId = createContainerResponse.getId();

        /* 4.在容器中执行代码，得到输出结果*/
        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();
        // 用于收集输出信息
        List<ExecuteMessage> executeMessageList = new ArrayList<>();

        // docker exec eager_germain java -cp /app Main 114 514
        for (String inputArgs : inputList) {
            StopWatch stopWatch = new StopWatch();
            String[] inputArgsArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
            // 创建一个执行命令
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStdin(true)
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("创建执行命令：" + execCreateCmdResponse);
            // 创建一个ExecuteMessage类方便获取信息
            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] message = {null};
            final String[] errorMessage = {null};
            long time = 0L;
            final boolean[] timeout = {true};

            String id = execCreateCmdResponse.getId();
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {
                    // 当程序执行完成时会调用这个方法，
                    // 如果程序执行完成时最大时间小于设定的超时时间，那么就设定为不超时
                    timeout[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    System.out.println("调用了运行容器时的回调函数！！！！！！114514");
                    StreamType streamType = frame.getStreamType();
                    System.out.println("流类型：" + streamType);

                    byte[] payload = frame.getPayload();
                    System.out.println("帧内容：" + new String(payload));

                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            final long[] maxMemory = {0L};

            StatsCmd statsCmd = dockerClient.statsCmd(containerId); //创建监控
            ResultCallback<Statistics> statisticsResultCallback = new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    Long usage = statistics.getMemoryStats().getUsage();
                    System.out.println("内存占用：" + usage);
                    maxMemory[0] = Math.max(usage, maxMemory[0]);
                }

                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }

                @Override
                public void close() throws IOException {

                }
            };
            statsCmd.exec(statisticsResultCallback); // 开启监控


            try {
                stopWatch.start();
                dockerClient.execStartCmd(id)
                        .exec(execStartResultCallback)
//                        .awaitCompletion(); // 设置超时时间
                        .awaitCompletion(TIME_OUT, TimeUnit.MINUTES); // 设置超时时间
                stopWatch.stop(); // 停止计时
                statsCmd.close(); // 关闭监控
                time = stopWatch.getLastTaskTimeMillis();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }

            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);

        }

        /*直接复制原来的代码*/
        /* 5.整理收集输出结果 */
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

        /* 6.文件清理*/
        if (userCodeFile.exists()) {
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("文件夹：" + userCodeParentPath + "\n清理" + (del ? "成功" : "失败"));
        }
        // 删除容器
        dockerClient.removeContainerCmd(containerId).withForce(true).exec();


        /* 7.错误处理，提升健壮性*/
        System.out.println("executeMessageList:" + executeMessageList);
        System.out.println("---------------------------------------------");
        System.out.println("executeCodeResponse" + executeCodeResponse);
        return null;
    }


    /**
     * @param e:
     * @return com.yybf.chenojcodesandbox.model.ExecuteCodeResponse:
     * @author yangyibufeng
     * @description 获取错误响应
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