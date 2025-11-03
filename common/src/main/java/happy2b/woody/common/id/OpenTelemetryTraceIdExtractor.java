package happy2b.woody.common.id;

import happy2b.woody.common.reflection.ReflectionUtils;
import happy2b.woody.common.utils.WoodyLog;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * opentelemetry trace id提取
 *
 * @author jiangjibo
 * @version 1.0
 * @since 2025/11/3
 */
public class OpenTelemetryTraceIdExtractor implements IdGenerator<String> {

    public static final OpenTelemetryTraceIdExtractor INSTANCE = new OpenTelemetryTraceIdExtractor();

    private int order;

    private Method optlCurrentContextMethod;

    private Class sdkSpanClass;

    public OpenTelemetryTraceIdExtractor() {
        this.order = ORDER.incrementAndGet();
    }

    @Override
    public String generateTraceId() {
        if (optlCurrentContextMethod == null) {
            try {
                Class clazz = Class.forName("io.opentelemetry.context.Context");
                optlCurrentContextMethod = ReflectionUtils.findMethod(clazz, "current");
            } catch (Exception e) {
                WoodyLog.error(e, "Woody: Failed to find method 'io.opentelemetry.context.Context.current()'!");
                return UUID.randomUUID().toString();
            }
        }

        try {
            Object context = ReflectionUtils.invokeMethod(optlCurrentContextMethod, null);
            if (context == null) {
                throw new IllegalStateException("Woody: Failed to get current optl context!");
            }
            if (!context.getClass().getName().endsWith("ArrayBasedContext")) {
                throw new IllegalStateException("Woody: Open context must be an ArrayBasedContext! Not support current optl version!");
            }
            Object[] entries = ReflectionUtils.get(context, "entries");
            if (entries == null) {
                throw new IllegalStateException("Woody: Failed to get entries from optl context!");
            }
            Object spanContext = null;
            for (int i = 0; i < entries.length; i += 2) {
                Object value = entries[i + 1];
                if (value == null) {
                    continue;
                }
                if (sdkSpanClass != null) {
                    if (value.getClass() == sdkSpanClass) {
                        spanContext = ReflectionUtils.get(value, "context");
                        break;
                    }
                } else {
                    if (value.getClass().getName().endsWith("SdkSpan")) {
                        sdkSpanClass = value.getClass();
                        spanContext = ReflectionUtils.get(value, "context");
                        break;
                    }
                }
            }
            if (spanContext == null) {
                WoodyLog.error("Woody: Failed to get span context from optl context!");
                return UUID.randomUUID().toString();
            }
            return ReflectionUtils.invoke(spanContext, "getTraceId");
        } catch (Exception e) {
            WoodyLog.error(e, "Woody: Failed to get trace id from optl context!");
            return UUID.randomUUID().toString();
        }
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
