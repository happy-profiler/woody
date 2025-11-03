package happy2b.woody.core.fetcher;

import happy2b.woody.common.api.Config;
import happy2b.woody.common.api.ResourceMethod;
import happy2b.woody.common.constant.ProfilingResourceType;
import happy2b.woody.common.reflection.ReflectionUtils;
import happy2b.woody.common.utils.WoodyLog;
import happy2b.woody.core.tool.jni.AsyncProfiler;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/21
 */
public class KafkaResourceFetcher extends AbstractResourceFetcher {

    public static final KafkaResourceFetcher INSTANCE = new KafkaResourceFetcher();

    private KafkaResourceFetcher() {
    }

    @Override
    public String[] getResourceClassName() {
        return ProfilingResourceType.KAFKA.getResourceClasses();
    }

    @Override
    public void fetchResources(Class clazz) {
        try {
            storeAppClassLoader(clazz);
            Object[] instances = AsyncProfiler.getInstance().getInstances(clazz, 100);
            if (instances == null || instances.length == 0) {
                WoodyLog.error("Woody: Failed to fetch kafka '{}' instance!", clazz.getName());
                return;
            }
            for (Object instance : instances) {
                Method method = ReflectionUtils.invoke(instance, "getMethod");
                if (method == null) {
                    WoodyLog.error("Woody: Failed to fetch kafka '{}' method!", instance.getClass().getName());
                    return;
                }
                Collection<String> topics = ReflectionUtils.invoke(instance, "getTopics");
                addResourceMethod(new ResourceMethod("kafka", "Consume Topic " + String.join(",", topics), method));
            }
        } catch (Throwable e) {
            WoodyLog.error("Woody: Fetch kafka resource occur exception!", e);
        }
    }

    @Override
    public ProfilingResourceType resourceType() {
        return ProfilingResourceType.KAFKA;
    }

}
