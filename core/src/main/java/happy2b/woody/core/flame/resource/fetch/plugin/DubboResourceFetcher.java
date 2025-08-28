package happy2b.woody.core.flame.resource.fetch.plugin;

import happy2b.woody.common.reflection.ReflectionUtils;
import happy2b.woody.common.utils.WoodyLog;
import happy2b.woody.core.flame.common.constant.ProfilingResourceType;
import happy2b.woody.core.flame.resource.ResourceMethod;
import happy2b.woody.core.flame.resource.fetch.ResourceFetcher;
import happy2b.woody.core.tool.jni.AsyncProfiler;

import java.lang.reflect.Method;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/21
 */
public class DubboResourceFetcher implements ResourceFetcher {


    public static final DubboResourceFetcher INSTANCE = new DubboResourceFetcher();

    private DubboResourceFetcher() {
    }

    @Override
    public String[] getResourceClassName() {
        return ProfilingResourceType.DUBBO.getResourceClasses();
    }

    @Override
    public void fetchResources(Class clazz) {
        try {
            Object[] instances = AsyncProfiler.getInstance().getInstances(clazz, 100);
            if (instances == null || instances.length == 0) {
                WoodyLog.error("Woody: Failed to fetch dubbo '{}' instance!", clazz.getName());
                return;
            }
            for (Object instance : instances) {
                Class<?> interfaceClass = ReflectionUtils.get(instance, "interfaceClass");
                Object ref = ReflectionUtils.get(instance, "ref");
                extractApacheDubboResources(interfaceClass, ref.getClass().getName());
            }
        } catch (Throwable e) {
            WoodyLog.error("Woody: Fetch http profiling resource occur exception", e);
        }
    }

    @Override
    public ProfilingResourceType resourceType() {
        return ProfilingResourceType.DUBBO;
    }

    private void extractApacheDubboResources(Class type, String implement) {
        if (implement.startsWith("org.apache.dubbo.") || implement.startsWith("com.alibaba.dubbo.")) {
            return;
        }
        try {
            Class<?> implementClass = type.getClassLoader().loadClass(implement);
            for (Method method : type.getDeclaredMethods()) {
                String methodPath = implement + "." + method.getName();
                String resource = buildRPCResource(type, method);
                if (resources.containsKey(resource)) {
                    return;
                }
                resources.put(resource, methodPath);
                Method m = implementClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
                addResourceMethod(new ResourceMethod("dubbo", resource, m));
            }
        } catch (Exception e) {
            WoodyLog.error("Load dubbo class failed!", e);
        }
    }

    private String buildRPCResource(Class type, Method method) {
        return type.getName() + "/" + method.getName();
    }
}
