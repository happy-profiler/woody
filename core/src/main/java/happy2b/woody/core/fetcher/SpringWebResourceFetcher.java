package happy2b.woody.core.fetcher;

import happy2b.woody.common.api.ResourceMethod;
import happy2b.woody.common.constant.ProfilingResourceType;
import happy2b.woody.common.reflection.ReflectionUtils;
import happy2b.woody.common.utils.WoodyLog;
import happy2b.woody.core.tool.jni.AsyncProfiler;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/21
 */
public class SpringWebResourceFetcher extends AbstractResourceFetcher {

    public static final SpringWebResourceFetcher INSTANCE = new SpringWebResourceFetcher();

    private SpringWebResourceFetcher() {
    }

    @Override
    public String[] getResourceClassName() {
        return ProfilingResourceType.SPRING_WEB.getResourceClasses();
    }

    @Override
    public void fetchResources(Class clazz) {
        try {
            Object[] instances = AsyncProfiler.getInstance().getInstances(clazz, 1);
            if (instances == null || instances.length == 0) {
                WoodyLog.error("Woody: Failed to fetch spring web '{}' instance!", clazz.getName());
                return;
            }
            Map<?, ?> handlerMethods = ReflectionUtils.invoke(instances[0], "getHandlerMethods");
            fetchSpringWebResources(handlerMethods);
        } catch (Throwable e) {
            WoodyLog.error("Woody: Fetch http profiling resource occur exception", e);
        }
    }

    @Override
    public ProfilingResourceType resourceType() {
        return ProfilingResourceType.SPRING_WEB;
    }

    private void fetchSpringWebResources(Map<?, ?> handlerMethods) {
        try {
            for (Map.Entry<?, ?> entry : handlerMethods.entrySet()) {
                Object mappingInfo = entry.getKey();
                String path = mappingInfo.toString();
                path = path.substring(1, path.length() - 1);
                if (!path.contains("/")) {
                    path += " /";
                }
                Object handlerMethod = entry.getValue();
                Method method = ReflectionUtils.get(handlerMethod, "method");
                extractHttpResources(path, method);
            }
        } catch (Exception e) {
            WoodyLog.error("One-Profiler: Fetch http profiling resource occur exception", e);
        }
    }

    private void extractHttpResources(String resource, Method method) {
        String methodPath = method.getDeclaringClass().getName() + "." + method.getName();
        if (resources.putIfAbsent(resource, methodPath) == null) {
            addResourceMethod(new ResourceMethod(ProfilingResourceType.SPRING_WEB.getValue(), resource, method));
        }
    }
}
