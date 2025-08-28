package happy2b.woody.core;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import happy2b.woody.core.config.Configure;
import happy2b.woody.common.utils.WoodyLog;
import happy2b.woody.common.utils.JavaVersionUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/18
 */
public class Woody {

    public static void main(String[] args) throws Exception {
        Configure configure = new Configure();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-core")) {
                configure.setWoodyCore(args[++i]);
                continue;
            }
            if (arg.equals("-agent")) {
                configure.setWoodyAgent(args[++i]);
                continue;
            }
            if (arg.equals("-pid")) {
                configure.setJavaPid(Integer.parseInt(args[++i]));
                continue;
            }
            if (arg.equals("-serverPort")) {
                configure.setServerPort(Integer.parseInt(args[++i]));
            }
        }

        attachAgent(configure);
    }

    private static void attachAgent(Configure configure) throws Exception {
        VirtualMachineDescriptor virtualMachineDescriptor = null;
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            String pid = descriptor.id();
            if (pid.equals(Long.toString(configure.getJavaPid()))) {
                virtualMachineDescriptor = descriptor;
                break;
            }
        }
        VirtualMachine virtualMachine = null;
        try {
            if (null == virtualMachineDescriptor) { // 使用 attach(String pid) 这种方式
                virtualMachine = VirtualMachine.attach("" + configure.getJavaPid());
            } else {
                virtualMachine = VirtualMachine.attach(virtualMachineDescriptor);
            }

            Properties targetSystemProperties = virtualMachine.getSystemProperties();
            String targetJavaVersion = JavaVersionUtils.javaVersionStr(targetSystemProperties);
            String currentJavaVersion = JavaVersionUtils.javaVersionStr();
            if (targetJavaVersion != null && currentJavaVersion != null) {
                if (!targetJavaVersion.equals(currentJavaVersion)) {
                    WoodyLog.warn("Current VM java version: {} do not match target VM java version: {}, attach may fail.",
                            currentJavaVersion, targetJavaVersion);
                    WoodyLog.warn("Target VM JAVA_HOME is {}, woody-boot JAVA_HOME is {}, try to set the same JAVA_HOME.",
                            targetSystemProperties.getProperty("java.home"), System.getProperty("java.home"));
                }
            }

            String woodyAgentPath = configure.getWoodyAgent();
            //convert jar path to unicode string
            configure.setWoodyAgent(encodeArg(woodyAgentPath));
            configure.setWoodyCore(encodeArg(configure.getWoodyCore()));
            try {
                virtualMachine.loadAgent(woodyAgentPath, configure.getWoodyCore() + ";" + configure);
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("Non-numeric value found")) {
                    WoodyLog.warn(e);
                    WoodyLog.warn("It seems to use the lower version of JDK to attach the higher version of JDK.");
                    WoodyLog.warn(
                            "This error message can be ignored, the attach may have been successful, and it will still try to connect.");
                } else {
                    throw e;
                }
            } catch (com.sun.tools.attach.AgentLoadException ex) {
                if ("0".equals(ex.getMessage())) {
                    // https://stackoverflow.com/a/54454418
                    WoodyLog.warn(ex);
                    WoodyLog.warn("It seems to use the higher version of JDK to attach the lower version of JDK.");
                    WoodyLog.warn(
                            "This error message can be ignored, the attach may have been successful, and it will still try to connect.");
                } else {
                    throw ex;
                }
            }
        } finally {
            if (null != virtualMachine) {
                virtualMachine.detach();
            }
        }
    }

    private static String encodeArg(String arg) {
        try {
            return URLEncoder.encode(arg, "utf-8");
        } catch (UnsupportedEncodingException e) {
            return arg;
        }
    }
}
