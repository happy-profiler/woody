package happy2b.woody.inst.core;

import happy2b.woody.common.utils.InstrumentationUtils;
import happy2b.woody.common.utils.MethodUtil;
import happy2b.woody.common.api.Config;
import happy2b.woody.common.api.InstrumentationProfiler;
import happy2b.woody.inst.asm.MethodLineTransformer;
import happy2b.woody.inst.asm.ProfilingTransformer;
import happy2b.woody.common.api.MethodIntrospection;
import happy2b.woody.inst.stats.MethodProfilingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class InstrumentationProfilingProcessor implements InstrumentationProfiler {

    private static final Logger log = LoggerFactory.getLogger(InstrumentationProfilingProcessor.class);

    private static final String EXCLUDE_PACKAGES_KEY = "excludePackages";
    private static final String LINE_SPAN_NUM_LIMIT_KEY = "lineSpanNumLimit";

    private List<String> CLASS_EXCLUDES = Arrays.asList("java.**", "ch.qos.logback.**", "org.slf4j.**");

    // class关联的transformers,方便卸载
    private static final Map<Class, Set<Method>> classTransformedMethods = new ConcurrentHashMap<>();
    private static final Map<Class, ClassFileTransformer> classTransformers = new ConcurrentHashMap<>();

    // 每个transformed method 的描述
    private static final Set<String> transformMethodDescriptors = new HashSet<>();

    // 方法被引用增强的次数
    private static final Map<Method, AtomicInteger> methodTransformReferences = new ConcurrentHashMap<>();

    private static final Map<Method, Set<Method>> internalMethods = new ConcurrentHashMap<>();

    private Thread profilingWorker;

    public InstrumentationProfilingProcessor() {
        profilingWorker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    MethodProfilingManager.nextDuration();
                    try {
                        long now = System.currentTimeMillis();
                        long nextMinute = (now / 60000 + 1) * 60000;
                        Thread.sleep(nextMinute - now);
                    } catch (InterruptedException e) {
                        log.warn("[databuff-resource-monitor]: Monitor thread is interrupted !", e);
                    }
                }
            }
        });
        profilingWorker.setDaemon(true);
        profilingWorker.start();
    }

    @Override
    public boolean profiling(Method method) throws Throwable {
        return doMethodProfiling(method.getDeclaringClass(), Collections.singleton(method));
    }

    @Override
    public boolean profiling(Class clazz, Set<Method> methodSet) throws Throwable {
        return doMethodProfiling(clazz, methodSet);
    }

    @Override
    public Set<Method> getProfilingMethods() {
        return Collections.emptySet();
    }

    @Override
    public boolean resetProfiling(Method method) throws Throwable {
        return false;
    }

    @Override
    public boolean resetProfiling(Class<?> clazz) throws Throwable {
        return false;
    }

    @Override
    public boolean resetAllProfiling() throws Throwable {
        return false;
    }

    @Override
    public String getMethodSourceCode(Method method) {
        return Config.getMethodLineIntrospection(method).getSourceCode();
    }

    @Override
    public String getMethodProfilingStats(Method method, int latestDurationInMin) {
        return MethodProfilingManager.getProfilingStats(method, latestDurationInMin);
    }

    private boolean doMethodProfiling(Class clazz, Set<Method> methods) {
        Set<Method> methodSet = classTransformedMethods.get(clazz);
        if (methodSet != null) {
            methodSet.removeAll(methods);
        }
        introspectClassMethodLines(clazz, methods, true);
        return doTransform(methods, false, false);
    }

    private void processInternalMethodsTransformations() {
        Map<Method, Set<Method>> configInternalMethods = Config.get().getPendingTransformMethods();
        if (configInternalMethods.isEmpty()) {
            return;
        }

        for (Map.Entry<Method, Set<Method>> entry : configInternalMethods.entrySet()) {
            internalMethods.computeIfAbsent(entry.getKey(), method -> new HashSet<>()).addAll(entry.getValue());
        }

        Map<Class, List<Method>> classMethods = configInternalMethods.entrySet().stream()
                .filter(new Predicate<Map.Entry<Method, Set<Method>>>() {
                    @Override
                    public boolean test(Map.Entry<Method, Set<Method>> methodSetEntry) {
                        return transformMethodDescriptors.contains(MethodUtil.getFullMethodDescriptor(methodSetEntry.getKey()));
                    }
                })
                .flatMap(new Function<Map.Entry<Method, Set<Method>>, Stream<Method>>() {
                    @Override
                    public Stream<Method> apply(Map.Entry<Method, Set<Method>> methodSetEntry) {
                        return methodSetEntry.getValue().stream();
                    }
                }).collect(Collectors.groupingBy(new Function<Method, Class>() {
                    @Override
                    public Class apply(Method method) {
                        return method.getDeclaringClass();
                    }
                }));

        for (Map.Entry<Class, List<Method>> entry : classMethods.entrySet()) {
            introspectClassMethodLines(entry.getKey(), entry.getValue(), true);
            List<Method> methods = entry.getValue();
            filterMethodByLineIntrospection(methods);
            if (!methods.isEmpty()) {
                doTransform(new HashSet<>(methods), false, false);
            }
        }

        configInternalMethods.clear();
    }

    private void filterMethodByLineIntrospection(List<Method> methods) {
        Iterator<Method> iterator = methods.iterator();
        while (iterator.hasNext()) {
            Method method = iterator.next();
            MethodIntrospection introspection = Config.get().getMethodLineIntrospection(method);
            if (introspection == null) {
                log.error("Cat`t find method line introspection for method:{}#{} ", method.getDeclaringClass().getName(), method.getName());
                iterator.remove();
                continue;
            }
            if (!introspection.isInstApplicable()) {
                log.info("Filter instrumentation method: {}#{}", method.getDeclaringClass().getName(), method.getName());
                iterator.remove();
            }
        }
    }

    private void introspectClassMethodLines(Class clazz, Collection<Method> methods, boolean introspectAnonymous) {
        if (!Config.get().hasMethodsIntrospected(clazz, methods)) {
            InstrumentationUtils.retransformClasses(new MethodLineTransformer(clazz, methods, CLASS_EXCLUDES), Arrays.asList(clazz));
        }

        if (!introspectAnonymous) {
            return;
        }

        Set<Class> anonymousClasses = new HashSet<>();
        Set<MethodIntrospection> introspections = Config.get().getMethodLineIntrospections(clazz);
        for (MethodIntrospection introspection : introspections) {
            Collection<Class> classes = introspection.getAnonymousClass();
            if (classes.isEmpty()) {
                continue;
            }
            anonymousClasses.addAll(classes);
        }

        if (anonymousClasses.isEmpty()) {
            return;
        }

        for (Class annClass : anonymousClasses) {
            Method[] annMethods = annClass.getDeclaredMethods();
            Set<Method> methodSet = Arrays.stream(annMethods).filter(method -> {
                int i = method.getModifiers() & 0x0040;
                return i == 0;
            }).collect(Collectors.toSet());
            introspectClassMethodLines(annClass, methodSet, false);
        }

    }


    /**
     * transform class
     *
     * @param methods
     * @param override   是否覆盖
     * @param introspect 是否内省
     */
    private boolean doTransform(Set<Method> methods, boolean override, boolean introspect) {
        Class<?> clazz = methods.iterator().next().getDeclaringClass();
        Config.get().refreshMethodLineIntrospections(clazz, methods);

        List<String> methodNames = methods.stream().map(method -> method.getName()).collect(Collectors.toList());
        try {
            Set<Method> transformedMethods;

            if (override) {
                transformedMethods = classTransformedMethods.computeIfAbsent(clazz, aClass -> new HashSet<>());
                transformedMethods.clear();
                transformedMethods.addAll(methods);
                removeClassTransformers(classTransformers.remove(clazz));
            } else {
                transformedMethods = classTransformedMethods.computeIfAbsent(clazz, aClass -> new HashSet<>());
                if (transformedMethods.containsAll(methods)) {
                    log.info("Already transform class '{}' method '{}'!", clazz.getName(), methodNames);
                    for (Method method : methods) {
                        methodTransformReferences.computeIfAbsent(method, method1 -> new AtomicInteger(0)).incrementAndGet();
                    }
                    return true;
                } else {
                    transformedMethods.addAll(methods);
                    removeClassTransformers(classTransformers.remove(clazz));
                }
            }

            ProfilingTransformer transformer = new ProfilingTransformer(clazz, new HashSet<>(methods));
            boolean success = InstrumentationUtils.retransformClasses(transformer, clazz);
            if (success) {
                return false;
            }

            classTransformers.put(clazz, transformer);

            for (Method method : methods) {
                transformMethodDescriptors.add(MethodUtil.getFullMethodDescriptor(method));
                if (!override) {
                    methodTransformReferences.computeIfAbsent(method, method1 -> new AtomicInteger(0)).incrementAndGet();
                }
            }

            log.info("Transform resource class:'{}' success!", clazz.getName());
        } catch (Throwable e) {
            log.error("Transform class '{}' occur exception!", clazz.getName(), e);
            try {
                ClassFileTransformer transformer = classTransformers.get(clazz);
                if (transformer != null) {
                    resetTransform(clazz, transformer);
                }
            } catch (UnmodifiableClassException ex) {
                log.error("Transform class '{}' failed, even reset it also occur exception!", clazz.getName(), e);
            }
            return false;
        }
        return true;
    }

    private void removeClassTransformers(ClassFileTransformer transformer) {
        if (transformer == null) {
            return;
        }
        InstrumentationUtils.getInstrumentation().removeTransformer(transformer);
    }

    private ClassFileTransformer transformWithInvokeInternal(Class clazz, Collection<Method> methods) {
        ProfilingTransformer transformer = new ProfilingTransformer(clazz, new HashSet<>(methods));
        InstrumentationUtils.retransformClasses(transformer, clazz);
        return transformer;
    }

    private String[] toTypeNames(Method method) {
        Class<?>[] classes = method.getParameterTypes();
        if (classes.length == 0) {
            return new String[0];
        }
        String[] types = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            types[i] = classes[i].getName();
        }
        return types;
    }

    private Method chooseOptimalMethod(List<Method> methods) {
        return methods.stream().sorted(new Comparator<Method>() {
            @Override
            public int compare(Method m1, Method m2) {
                Class<?>[] types1 = m1.getParameterTypes();
                Class<?>[] types2 = m2.getParameterTypes();
                if (types1.length != types2.length) {
                    return types2.length - types1.length;
                }
                for (int i = 0; i < types1.length; i++) {
                    Class c1 = types1[i];
                    Class c2 = types2[i];
                    if (c1 == Object.class && c2 != Object.class) {
                        return 1;
                    }
                    if (c1 != Object.class && c2 == Object.class) {
                        return -1;
                    }
                }
                return 0;
            }
        }).collect(Collectors.toList()).get(0);
    }

    private void uninstallAllTransformations() {
        for (Map.Entry<Class, ClassFileTransformer> entry : classTransformers.entrySet()) {
            Class clazz = entry.getKey();
            try {
                resetTransform(entry.getKey(), entry.getValue());
                log.info("Reset class:'{}' for resource success!", clazz.getName());
            } catch (Throwable throwable) {
                log.error("Reset class:'{}' failed", clazz.getName(), throwable);
            }
        }
        clearMethodCaches();
    }

    private void resetClass(Class clazz) {
        try {
            resetTransform(clazz, classTransformers.remove(clazz));
            log.error("Reset class '{}' success!", clazz.getName());
        } catch (Exception e) {
            log.error("Reset class '{}' occur exception!", clazz.getName(), e);
        }
    }

    public static void resetTransform(Class<?> clazz, ClassFileTransformer transformer) throws UnmodifiableClassException {
        InstrumentationUtils.getInstrumentation().removeTransformer(transformer);
        InstrumentationUtils.getInstrumentation().retransformClasses(clazz);
    }

    private void decrementMethodReference(Method method, Set<Method> needResetMethods) {
        int ref = methodTransformReferences.get(method).decrementAndGet();
        if (ref == 0) {
            needResetMethods.add(method);
            transformMethodDescriptors.remove(MethodUtil.getFullMethodDescriptor(method));
        }
        Set<Method> methods = internalMethods.get(method);
        if (methods != null) {
            for (Method m : methods) {
                decrementMethodReference(m, needResetMethods);
            }
        }
    }

    private void clearMethodCaches() {
        classTransformedMethods.clear();
        classTransformers.clear();
        methodTransformReferences.clear();
        transformMethodDescriptors.clear();
        internalMethods.clear();
    }

}
