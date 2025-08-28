package happy2b.woody.common.bytecode;

import happy2b.woody.common.utils.WoodyLog;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;

public class SourceCodeExtractor {

    private static final String PROFILING_CLASSES = "profiling_classes";

    private static File AGENT_DIR;
    private static File PROFILING_SPAN_CLASS;

    private static synchronized void init(Class agentClass) {
        if (AGENT_DIR == null) {
            AGENT_DIR = new File(agentClass.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile();
            PROFILING_SPAN_CLASS = new File(AGENT_DIR, PROFILING_CLASSES);
        }
    }

    public static String extractSourceCode(Class clazz) {
        try {
            return Decompiler.decompile(getClassFilePath(clazz), null);
        } catch (Exception e) {
            WoodyLog.error("Failed extract method {} source code!", clazz.getName());
            return null;
        }
    }

    public static String extractSourceCode(Method method) {
        try {
            String fileName = getClassFilePath(method.getDeclaringClass());
            return Decompiler.decompile(fileName, method);
        } catch (Exception e) {
            WoodyLog.error("Failed extract method {}#{} source code!", method.getDeclaringClass().getName(), method.getName());
            return null;
        }
    }

    public static boolean saveSourceCode(Class operator, String className, byte[] classfileBuffer) {
        if (AGENT_DIR == null) {
            init(operator);
        }
        if (className == null) {
            WoodyLog.error("Can`t save source code with null name!");
            return false;
        }
        if (!PROFILING_SPAN_CLASS.exists()) {
            PROFILING_SPAN_CLASS.mkdirs();
        }
        try {
            File saveFile = new File(PROFILING_SPAN_CLASS, className + ".class");
            if (saveFile.exists()) {
                byte[] bytes = Files.readAllBytes(saveFile.toPath());
                if (bytes.length == classfileBuffer.length) {
                    WoodyLog.info("Class for name {} source file is exist!", className);
                    return true;
                }
                boolean delete = saveFile.delete();
                if (delete) {
                    WoodyLog.info("Class for name {} source file is expired, delete it!", className);
                } else {
                    WoodyLog.error("Class for name {} source file is expired, failed to delete it!", className);
                    return false;
                }
            }

            File dir = new File(PROFILING_SPAN_CLASS, className.substring(0, className.lastIndexOf("/")));
            if (!dir.exists()) {
                dir.mkdirs();
            }

            boolean created = saveFile.createNewFile();
            if (!created) {
                WoodyLog.error("Failed to create new source file for class {} !", className);
                return false;
            }
            Files.write(saveFile.toPath(), classfileBuffer);
            return true;
        } catch (Exception e) {
            WoodyLog.error("Save class {} source code occur exception!", className, e);
        }
        return false;
    }

    private static String getClassFilePath(Class clazz) {
        return PROFILING_SPAN_CLASS.getAbsolutePath() + File.separator + clazz.getName().replace(".", "/") + ".class";
    }
}
