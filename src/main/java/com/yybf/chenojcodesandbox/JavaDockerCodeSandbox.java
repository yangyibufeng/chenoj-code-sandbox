package com.yybf.chenojcodesandbox;

import cn.hutool.core.date.StopWatch;
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
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 通过复用模板来编写通过Docker实现代码沙箱
 *
 * @author yangyibufeng
 * @date 2024/3/1
 */
@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {
    public static final Boolean FIRST_INIT = true;

    /**
     * @description 重写了模板中的运行文件类，将其修改为通过Docker的Java镜像来运行用户代码文件的方式
     * @param inputList: 测试用例
     * @return java.util.List<com.yybf.chenojcodesandbox.model.ExecuteMessage>:
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
        System.out.println("下载完成");

        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        hostConfig.setBinds(new Bind(USER_CODE_PARENT_PATH, new Volume("/app")));
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

        return executeMessageList;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}