package happy2b.woody.boot;

import happy2b.woody.common.utils.WoodyLog;
import happy2b.woody.common.utils.PortUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.InputMismatchException;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/15
 */
public class Bootstrap {

    private static final int SERVER_BINDING_PORT = 9050;

    public static void main(String[] args) throws Exception {
        long pid = 0;
        // select pid
        try {
            pid = ProcessUtils.select(false);
        } catch (InputMismatchException e) {
            System.out.println("Please input an integer to select pid.");
            System.exit(1);
        }
        if (pid < 0) {
            System.out.println("Please select an available pid.");
            System.exit(1);
        }
        File woodyHomeDir = null;
        CodeSource codeSource = Bootstrap.class.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            File bootJarPath = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
            List<String> modules = extractModules(bootJarPath);
            woodyHomeDir = bootJarPath.getParentFile();
            for (String module : modules) {
                extractJarFile(module, module.substring(module.indexOf("/") + 1, module.lastIndexOf("-")) + ".jar", woodyHomeDir);
            }
        }

        int availablePort = PortUtils.getAvailablePort(SERVER_BINDING_PORT);

        List<String> attachArgs = new ArrayList<>();
        attachArgs.add("-jar");
        attachArgs.add(new File(woodyHomeDir, "woody-core.jar").getAbsolutePath());
        attachArgs.add("-pid");
        attachArgs.add("" + pid);
        attachArgs.add("-core");
        attachArgs.add(new File(woodyHomeDir, "woody-core.jar").getAbsolutePath());
        attachArgs.add("-agent");
        attachArgs.add(new File(woodyHomeDir, "woody-agent.jar").getAbsolutePath());
        attachArgs.add("-serverPort");
        attachArgs.add(availablePort + "");
        WoodyLog.info("Try to attach process " + pid);
        WoodyLog.debug("Start woody-core.jar args: " + attachArgs);
        ProcessUtils.startWoodyCore(pid, attachArgs);

        WoodyLog.info("Attach process {} success.", pid);

        // 启动woody-client
        WoodyLog.info("woody-client connect {} {}", "127.0.0.1", availablePort);

        System.setProperty("jline.terminal", "org.jline.terminal.impl.PosixTerminal");
        System.setProperty("jline.terminal.dumb", "false");

        new WoodyClient("127.0.0.1", availablePort, pid).boot();
    }

    private static List<String> extractModules(File jarFile) throws IOException {
        List<String> modules = new ArrayList<>();
        JarFile jar = new JarFile(jarFile);
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (entryName.startsWith("modules/woody-") && !entry.isDirectory() && entryName.endsWith("jar")) {
                modules.add(entryName);
            }
        }
        return modules;
    }

    private static File extractJarFile(String modulePath, String moduleName, File woodyHomeDir) throws IOException {
        File coreJar = new File(woodyHomeDir, moduleName);
        if (coreJar.exists()) {
            coreJar.delete();
        }
        String innerJarPath = "/" + modulePath;
        InputStream in = Bootstrap.class.getResourceAsStream(innerJarPath);
        if (in == null) {
            throw new IllegalStateException("can not find jar: " + innerJarPath);
        }
        Files.copy(in, coreJar.toPath());
        coreJar.deleteOnExit();
        return coreJar;
    }
}
