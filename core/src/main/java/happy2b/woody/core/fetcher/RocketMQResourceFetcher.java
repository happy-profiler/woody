package happy2b.woody.core.fetcher;

import happy2b.woody.common.api.Config;
import happy2b.woody.common.api.ResourceMethod;
import happy2b.woody.common.constant.ProfilingResourceType;
import happy2b.woody.common.reflection.ReflectionUtils;
import happy2b.woody.common.utils.WoodyLog;
import happy2b.woody.core.tool.jni.AsyncProfiler;

import java.util.Map;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/21
 */
public class RocketMQResourceFetcher extends AbstractResourceFetcher {

    public static final RocketMQResourceFetcher INSTANCE = new RocketMQResourceFetcher();

    private RocketMQResourceFetcher() {
    }

    @Override
    public String[] getResourceClassName() {
        return ProfilingResourceType.ROCKETMQ.getResourceClasses();
    }

    @Override
    public void fetchResources(Class clazz) {
        storeAppClassLoader(clazz);
        if (clazz.getName().contains("spring")) {
            fetchResources1(clazz);
        } else {
            fetchResources2(clazz);
        }
    }

    @Override
    public ProfilingResourceType resourceType() {
        return ProfilingResourceType.ROCKETMQ;
    }

    private void fetchResources1(Class clazz) {
        try {
            Object[] instances = AsyncProfiler.getInstance().getInstances(clazz, 100);
            if (instances == null || instances.length == 0) {
                WoodyLog.error("Woody: Failed to fetch rocketmq '{}' instance!", clazz.getName());
                return;
            }
            for (Object instance : instances) {
                Object messageListener = ReflectionUtils.invoke(instance, "getRocketMQListener");
                fetchRocketMQResources_1(instance, messageListener);
            }
        } catch (Throwable e) {
            WoodyLog.error("Woody: Fetch rocketmq resource occur exception!", e);
        }
    }

    private void fetchRocketMQResources_1(Object container, Object listener) {
        String topic = ReflectionUtils.invoke(container, "getTopic");
        Class<?> type = listener.getClass();
        extractRocketMQResource(topic, type, "onMessage");
    }

    private void fetchResources2(Class clazz) {
        try {
            Object[] instances = AsyncProfiler.getInstance().getInstances(clazz, 100);
            if (instances == null || instances.length == 0) {
                WoodyLog.error("Woody: Failed to fetch rocketmq '{}' instance!", clazz.getName());
                return;
            }
            for (Object instance : instances) {
                Object messageListener = ReflectionUtils.get(instance, "messageListener");
                if (messageListener == null) {
                    WoodyLog.error("Woody: Failed to fetch rocketmq '{}' messageListener!", clazz.getName());
                    return;
                }
                fetchRocketMQResources_2(instance, messageListener);
            }
        } catch (Throwable e) {
            WoodyLog.error("Woody: Fetch rocketmq resource occur exception!", e);
        }
    }


    private void fetchRocketMQResources_2(Object consumer, Object listener) {
        Object innerConsumer = ReflectionUtils.get(consumer, "defaultMQPushConsumerImpl");
        Object rebalance = ReflectionUtils.invoke(innerConsumer, "getRebalanceImpl");
        Map<String, ?> topics = ReflectionUtils.invoke(rebalance, "getSubscriptionInner");
        extractRocketMQResource(
                String.join(",", topics.keySet()), listener.getClass(), "consumeMessage");
    }


    private void extractRocketMQResource(String topic, Class type, String method) {
        String typeName = type.getName();
        if (typeName.startsWith("org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer")) {
            return;
        }
        String resource = "Consume Topic " + topic;
        String methodPath = type.getName() + "." + method;
        if (resources.containsKey(resource)) {
            return;
        }
        resources.put(resource, methodPath);
        addResourceMethod(new ResourceMethod("rocketmq", resource, findMostApplicableMethod(type, method)));
    }

}
