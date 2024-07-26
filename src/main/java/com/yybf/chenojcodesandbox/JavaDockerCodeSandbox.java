package com.yybf.chenojcodesandbox;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.yybf.chenojcodesandbox.model.ExecuteCodeRequest;
import com.yybf.chenojcodesandbox.model.ExecuteCodeResponse;
import com.yybf.chenojcodesandbox.model.ExecuteMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 通过复用模板来编写通过Docker实现代码沙箱
 *
 * @author yangyibufeng
 * @date 2024/7/22
 */
@Component
@Slf4j
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {
    public static final Boolean FIRST_INIT = false;

    public static final long TIME_OUT = 5000L;

    public static void main(String[] args) {
        JavaDockerCodeSandbox codeSandbox = new JavaDockerCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        String code = ResourceUtil.readStr("testCode/simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/unsafeCode/RunFileError.java", StandardCharsets.UTF_8);
//        String code = ResourceUtil.readStr("testCode/simpleCompute/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    /**
     * @param inputList: 测试用例
     * @return java.util.List<com.yybf.chenojcodesandbox.model.ExecuteMessage>:
     * @description 重写了模板中的运行文件类，将其修改为通过Docker的Java镜像来运行用户代码文件的方式
     * @author yangyibufeng
     * @date 2024/3/1 19:03
     */
    @Override
    public List<ExecuteMessage> runFile(List<String> inputList) {

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
        System.out.println("镜像已加载完成");

        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        hostConfig.setBinds(new Bind(USER_CODE_PARENT_PATH, new Volume("/app")));
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L); // 限制内存与硬盘之间的数据交换
        hostConfig.withCpuCount(1L);
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
        log.info("容器id为：{}", containerId);

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

            StringBuffer outputBuffer = new StringBuffer();
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
                    System.out.println("timeout is:" + Arrays.toString(timeout));

                    // 检查message和errorMessage是否为空
                    // 用来兼容print这种类型
                    if (executeMessage.getMessage() == null && executeMessage.getErrorMessage() == null) {
                        // 如果message和errorMessage都为空，说明可能是System.out.print()的输出
                        if (outputBuffer!= null) {
                            String completeOutput = outputBuffer.toString().trim();
                            System.out.println("print - 完整输出：" + completeOutput);
                            executeMessage.setMessage(completeOutput);
                        }
                    }
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    System.out.println("调用了运行容器时的回调函数！！！！！！114514");
//                    log.info("调用了运行容器时的回调函数！！！！！！114514");

                    StreamType streamType = frame.getStreamType();
                    System.out.println("流类型：" + streamType);
//                    log.info("流类型：{}" , streamType);

                    byte[] payload = frame.getPayload();
                    System.out.println("帧内容：" + new String(payload));
//                    log.info("帧内容：{}" , new String(payload));

                    if (StreamType.STDOUT.equals(streamType)) {
                        String output = new String(payload, StandardCharsets.UTF_8);
                        outputBuffer.append(output);

                        // 检查是否有换行符，表示一个完整的输出
                        if (output.endsWith("\n")) {
                            String completeOutput = outputBuffer.toString().trim();
                            System.out.println("完整输出：" + completeOutput);
                            executeMessage.setMessage(completeOutput);
                            outputBuffer.setLength(0); // 清空缓冲区
                        }
                    } else if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        executeMessage.setErrorMessage(errorMessage[0]);
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    }
                    super.onNext(frame);
                }
            };

            final long[] maxMemory = {0L};

            StatsCmd statsCmd = dockerClient.statsCmd(containerId); //创建监控
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    Long usage = statistics.getMemoryStats().getUsage();
                    maxMemory[0] = Math.max(usage, maxMemory[0]);
                    System.out.println("内存占用统计：" + maxMemory[0]);
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
            });
            statsCmd.exec(statisticsResultCallback); // 开启监控

            try {
                stopWatch.start();
                dockerClient.execStartCmd(id)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MICROSECONDS); // 设置超时时间
//                        .awaitCompletion(); // 设置超时时间
                stopWatch.stop(); // 停止计时
                time = stopWatch.getLastTaskTimeMillis();
                log.info("time is:{}", time);
                statisticsResultCallback.close();
                statsCmd.close(); // 关闭监控
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            log.info("整理之前的返回信息：{}", executeMessageList);


            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time);
            System.out.println("maxmemory"+maxMemory[0]);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
            log.info("返回信息：{}", executeMessageList);

        }
        // 添加停止和删除容器的代码
        try {
            log.info("开始删除容器,id：{}", containerId);
            // 停止容器
            dockerClient.stopContainerCmd(containerId).exec();
            // 删除容器
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            log.info("成功删除容器");
        } catch (Exception e) {
            System.out.println("删除容器失败：" + e.getMessage());
            throw new RuntimeException(e);
        }

        return executeMessageList;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}