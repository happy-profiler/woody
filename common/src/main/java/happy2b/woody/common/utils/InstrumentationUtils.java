package happy2b.woody.common.utils;


import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class InstrumentationUtils {

    private InstrumentationUtils() {
    }

    private static final AtomicReference<Instrumentation> instrumentationRef =
            new AtomicReference<>();

    public static void setInstrumentation(Instrumentation instrumentation) {
        instrumentationRef.compareAndSet(null, instrumentation);
    }

    public static Instrumentation getInstrumentation() {
        return instrumentationRef.get();
    }

    public static boolean retransformClasses(ClassFileTransformer transformer, Class... classes) {
        Instrumentation inst = instrumentationRef.get();
        try {
            inst.addTransformer(transformer, true);
            inst.retransformClasses(classes);
            return true;
        } catch (UnmodifiableClassException e) {
            WoodyLog.error("[One-Profiler]: RetransformClasses class error, classes:{}", classes, e);
            inst.removeTransformer(transformer);
            return false;
        }
    }

    public static void retransformClasses(ClassFileTransformer transformer, List<Class<?>> classes) {
        Instrumentation inst = instrumentationRef.get();
        try {
            inst.addTransformer(transformer, true);
            for (Class<?> clazz : classes) {
                if (isLambdaClass(clazz)) {
                    continue;
                }
                try {
                    inst.retransformClasses(clazz);
                } catch (Throwable e) {
                    WoodyLog.error("[One-Profiler]: RetransformClasses class error, class:{}", clazz.getName(), e);
                }
            }
        } finally {
            inst.removeTransformer(transformer);
        }
    }

    public static boolean isLambdaClass(Class<?> clazz) {
        return clazz.getName().contains("$$Lambda$");
    }

    public static List<Class> findClass(String ... classNames) {
        Instrumentation instrumentation = instrumentationRef.get();
        if (instrumentation == null) {
            WoodyLog.error("[One-Profiler]: InstrumentationUtils: instrumentation is null");
            return null;
        }
        Set<String> nameSet = new HashSet<>(classNames.length);
        for (String className : classNames) {
            nameSet.add(className);
        }

        List<Class> classes = new ArrayList<>();
        for (Class clazz : instrumentation.getAllLoadedClasses()) {
            if (nameSet.contains(clazz.getName())) {
                classes.add(clazz);
            }
        }
        return classes;
    }
}
