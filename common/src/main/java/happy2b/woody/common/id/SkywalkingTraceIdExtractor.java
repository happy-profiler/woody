package happy2b.woody.common.id;

import happy2b.woody.common.api.Config;
import happy2b.woody.common.reflection.ReflectionUtils;
import happy2b.woody.common.utils.WoodyLog;

import java.util.UUID;

/**
 * skywalking traceId关联
 *
 * @author jiangjibo
 * @version 1.0
 * @since 2025/11/3
 */
public class SkywalkingTraceIdExtractor implements IdGenerator<String> {

    public static final SkywalkingTraceIdExtractor INSTANCE = new SkywalkingTraceIdExtractor();

    private int order;

    private ClassLoader appClassLoader;

    private Class ctxManagerClass;

    public SkywalkingTraceIdExtractor() {
        this.order = ORDER.incrementAndGet();
    }

    @Override
    public String generateTraceId() {
        if (this.appClassLoader == null) {
            this.appClassLoader = Config.INSTANCE.getAppClassLoader();
        }
        if (this.appClassLoader == null) {
            WoodyLog.error("appClassLoader is null");
            return UUID.randomUUID().toString();
        }
        if (ctxManagerClass == null) {
            try {
                this.ctxManagerClass = appClassLoader.loadClass("org.apache.skywalking.apm.agent.core.context.ContextManager");
            } catch (Exception e) {
                WoodyLog.error("Failed to load Skywalking class!", e);
                return UUID.randomUUID().toString();
            }
        }
        ThreadLocal contextThreadLocal = ReflectionUtils.getStatic(ctxManagerClass, "CONTEXT");
        Object context = contextThreadLocal.get();
        if (context == null) {
            WoodyLog.error("org.apache.skywalking.apm.agent.core.context.ContextManager#CONTEXT value is null");
            return UUID.randomUUID().toString();
        }
        return ReflectionUtils.invoke(context, "getReadablePrimaryTraceId");
    }

    @Override
    public String generateSpanId() {
        return "";
    }

    @Override
    public int getOrder() {
        return order;
    }
}
