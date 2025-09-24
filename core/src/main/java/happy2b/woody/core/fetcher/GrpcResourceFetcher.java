package happy2b.woody.core.fetcher;

import happy2b.woody.common.api.ResourceMethod;
import happy2b.woody.common.constant.ProfilingResourceType;
import happy2b.woody.common.reflection.ReflectionUtils;
import happy2b.woody.common.utils.WoodyLog;
import happy2b.woody.core.tool.jni.AsyncProfiler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/21
 */
public class GrpcResourceFetcher extends AbstractResourceFetcher{

    public static final GrpcResourceFetcher INSTANCE = new GrpcResourceFetcher();

    private GrpcResourceFetcher() {
    }

    @Override
    public String[] getResourceClassName() {
        return ProfilingResourceType.GRPC.getResourceClasses();
    }

    @Override
    public void fetchResources(Class clazz) {
        try {
            Object[] instances = AsyncProfiler.getInstance().getInstances(clazz, 3);
            if (instances == null || instances.length == 0) {
                WoodyLog.error("Woody: Failed to fetch grpc '{}' instance!", clazz.getName());
                return;
            }
            for (Object instance : instances) {
                List<Object> services = ReflectionUtils.getFieldValues(instance, "registry", "services");
                for (Object service : services) {
                    Map<String, ?> methods = ReflectionUtils.get(service, "methods");
                    for (Map.Entry<String, ?> entry : methods.entrySet()) {
                        String key = entry.getKey();
                        String[] split = key.split("/");
                        Method method = fetchGrpcResourceMethod(split[0], split[1], entry.getValue());
                        if (method == null) {
                            WoodyLog.error("Woody: Can not find method: " + split[1] + " in " + entry.getValue().getClass().getName());
                            return;
                        }
                        addResourceMethod(new ResourceMethod("grpc", key, method));
                    }
                }
            }
        } catch (Throwable e) {
            WoodyLog.error("Woody: Fetch http profiling resource occur exception", e);
        }
    }

    @Override
    public ProfilingResourceType resourceType() {
        return ProfilingResourceType.GRPC;
    }

    private Method fetchGrpcResourceMethod(String clazz, String methodName, Object service) {
        Object serviceImpl = ReflectionUtils.getFieldValues(service, "handler", "callHandler", "method", "serviceImpl");
        if (serviceImpl == null) {
            WoodyLog.error("Woody: ServiceImpl is null, class: " + clazz);
            return null;
        }
        List<Method> methods = ReflectionUtils.findMethodIgnoreParamTypes(serviceImpl.getClass(), methodName);
        for (Method method : methods) {
            if (method.getDeclaringClass().getName().contains("$") || !Modifier.isPublic(method.getModifiers()) || method.getParameterTypes().length != 2) {
                continue;
            }
            return method;
        }
        return null;
    }
}
