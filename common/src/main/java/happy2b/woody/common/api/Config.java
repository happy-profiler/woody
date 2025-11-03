package happy2b.woody.common.api;


import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Config {

    public static final Config INSTANCE = new Config();

    public static Config get() {
        return INSTANCE;
    }

    private String serviceName;

    private ClassLoader appClassLoader;

    private Map<Method, Set<Method>> pendingTransformMethods = new ConcurrentHashMap<>();

    private static Map<Integer, MethodIntrospection> methodLineIntrospections = new ConcurrentHashMap<>();
    private Map<Class, Set<Method>> introspectedClassMethods = new ConcurrentHashMap<>();

    public static boolean jmxMemAllocAvailable;

    static {
        try {
            Class.forName("com.sun.management.ThreadMXBean");
            ((com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean()).getThreadAllocatedBytes(Thread.currentThread().getId());
            jmxMemAllocAvailable = true;
        } catch (Exception e) {
            jmxMemAllocAvailable = false;
        }
    }

    public ClassLoader getAppClassLoader() {
        return appClassLoader;
    }

    public void setAppClassLoader(ClassLoader appClassLoader) {
        if (this.appClassLoader == null) {
            this.appClassLoader = appClassLoader;
        }
    }

    public Map<Method, Set<Method>> getPendingTransformMethods() {
        return pendingTransformMethods;
    }

    public void refreshMethodIntrospection(Method method, MethodIntrospection introspection) {
        methodLineIntrospections.put(method.hashCode(), introspection);
    }

    public String getServiceName() {
        return serviceName;
    }

    public void addLineIntrospectClass(Class clazz) {
        introspectedClassMethods.put(clazz, new HashSet<>());
    }

    public boolean hasMethodIntrospected(Class clazz, Method method) {
        return introspectedClassMethods.containsKey(clazz) && introspectedClassMethods.get(clazz).contains(method);
    }

    public void addMethodLineIntrospection(Method method, MethodIntrospection endpoint) {
        introspectedClassMethods.computeIfAbsent(method.getDeclaringClass(), new Function<Class, Set<Method>>() {
            @Override
            public Set<Method> apply(Class aClass) {
                return new HashSet<>();
            }
        }).add(method);
        methodLineIntrospections.put(method.hashCode(), endpoint);
    }

    public static final MethodIntrospection getMethodLineIntrospection(Method method) {
        return methodLineIntrospections.get(method.hashCode());
    }

    public boolean hasMethodsIntrospected(Class clazz, Collection<Method> methods) {
        return introspectedClassMethods.containsKey(clazz) && introspectedClassMethods.get(clazz).containsAll(methods);
    }

    public Set<MethodIntrospection> getMethodLineIntrospections(Class clazz) {
        Set<MethodIntrospection> introspections = new HashSet<>();
        Set<Method> methods = introspectedClassMethods.get(clazz);
        for (Method method : methods) {
            MethodIntrospection introspection = methodLineIntrospections.get(method.hashCode());
            if (introspection != null) {
                introspections.add(introspection);
            }
        }
        return introspections;
    }

    public void refreshMethodLineIntrospections(Class clazz, Set<Method> pendingRefreshes) {
        Set<Method> methods = introspectedClassMethods.get(clazz);
        if (methods == null || methods.isEmpty()) {
            return;
        }
        for (Method method : methods) {
            if (!pendingRefreshes.contains(method)) {
                continue;
            }
            MethodIntrospection introspection = methodLineIntrospections.get(method.hashCode());
            if (introspection != null) {
                introspection.refreshIntrospectLines();
            }
        }
    }

}
