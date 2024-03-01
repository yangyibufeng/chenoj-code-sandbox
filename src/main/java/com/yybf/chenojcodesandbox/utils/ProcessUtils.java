package com.yybf.chenojcodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.yybf.chenojcodesandbox.model.ExecuteMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 有关进程的工具类
 *
 * @author yangyibufeng
 * @date 2024/2/19
 */
public class ProcessUtils {

    /**
     * @param runProcess: 需要执行的进程
     * @param opName:     该进程的中文类型（编译/运行）
     * @return com.yybf.chenojcodesandbox.model.ExecuteMessage:
     * @author yangyibufeng
     * @description 用来获取执行进程中所产生的信息 （程序从args中获取参数）
     * @date 2024/2/19 18:30
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            // 等待程序执行，获取返回码
            int exitValue = runProcess.waitFor();

            executeMessage.setExitValue(exitValue);

            // 分批获取进程的正常输出
            // 会将控制台的信息写到进程的输入流中，所以先获取输入流，
            // 然后再创建一个输入流读取器读取输入流，最后在分块读取输入流
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
            List<String> outputStrList = new ArrayList<>();
            // 逐行读取
            String compileOutputLine;
            /* 这里逐行读取的时候会在最后加上一个换行符，可能就是docker代码沙箱中执行线程调用两次onNext的原因 */
//            while ((compileOutputLine = bufferedReader.readLine()) != null) {
//                compileOutputStringBuilder.append(compileOutputLine).append("\n");
//            }
            /*修改方法：先用一个arraylist数组存储每一个样例的输出，然后再用 Apache Commons Lang 库的 StringUtils 类中的join方法将
            数组中的元素被指定字符串分割之后再生成一个新的字符串*/
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                outputStrList.add(compileOutputLine);
            }

            // 获取正常输入的信息
            executeMessage.setMessage(StringUtils.join(outputStrList,"\n"));

            // 正常退出
            if (exitValue == 0) {
                System.out.println(opName + "成功");

                // 原本在这里获取正常输出


            } else {
                // 异常退出
                System.out.println(opName + "失败，错误码：" + exitValue);

                // 原本在这里获取正常输出

                // 分批获取进程的异常输出
                // 会将控制台的信息写到进程的输入流中，所以先获取输入流，
                // 然后再创建一个输入流读取器读取输入流，最后在分块读取输入流
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));


                List<String> errorOutputStrList = new ArrayList<>();
                // 逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorOutputStrList.add(errorCompileOutputLine);
                }

                // 获取异常返回信息
                executeMessage.setErrorMessage(StringUtils.join(errorOutputStrList,"\n"));
            }

            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
//            System.out.println(compileOutputStringBuilder);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return executeMessage;
    }

    /**
     * @param args:       需要进行测试的输入用例
     * @param runProcess: 需要执行的进程
     * @param opName:     该进程的中文类型（编译/运行）
     * @return com.yybf.chenojcodesandbox.model.ExecuteMessage:
     * @author yangyibufeng
     * @description 用来获取执行进程中所产生的信息 （程序从控制台获取参数，交互式，ACM）
     * @date 2024/2/19 19:25
     */
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String opName, String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            // 获取到输出流，向控制台写入测试用例
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            // 将字符串中间的空格替换为 \n ，并且在最后要再加上一个 \n
            String[] s = args.split(" ");
            String input = StrUtil.join("\n", s) + "\n";
            // 将拼接好的用例写入控制台，flush相当于用来发送
            outputStreamWriter.write(input);
            outputStreamWriter.flush();

            // 等待程序执行，获取返回码
            int exitValue = runProcess.waitFor();

            executeMessage.setExitValue(exitValue);

            // 分批获取进程的正常输出
            // 会将控制台的信息写到进程的输入流中，所以先获取输入流，
            // 然后再创建一个输入流读取器读取输入流，最后在分块读取输入流
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
            // 将分块读取的信息拼装起来
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            // 逐行读取
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }

            // 获取正常输入的信息
            executeMessage.setMessage(compileOutputStringBuilder.toString());

            // 正常退出
            if (exitValue == 0) {
                System.out.println(opName + "成功");

                // 原本在这里获取正常输出


            } else {
                // 异常退出
                System.out.println(opName + "失败，错误码：" + exitValue);

                // 原本在这里获取正常输出

                // 分批获取进程的异常输出
                // 会将控制台的信息写到进程的输入流中，所以先获取输入流，
                // 然后再创建一个输入流读取器读取输入流，最后在分块读取输入流
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                // 将分块读取的信息拼装起来
                StringBuilder errorCompileOutputStringBuilder = new StringBuilder();
                // 逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorCompileOutputStringBuilder.append(errorCompileOutputLine);
                }

                // 获取异常返回信息
                executeMessage.setErrorMessage(errorCompileOutputStringBuilder.toString());
            }

            // 回收资源
            outputStreamWriter.close();
            outputStream.close();
            runProcess.destroy();
//            System.out.println(compileOutputStringBuilder);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return executeMessage;
    }
}