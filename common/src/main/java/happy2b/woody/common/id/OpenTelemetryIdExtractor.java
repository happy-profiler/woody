package happy2b.woody.common.id;

import happy2b.woody.common.reflection.ReflectionUtils;
import happy2b.woody.common.utils.WoodyLog;

import java.lang.reflect.Method;

/**
 * opentelemetry trace id提取
 *
 * @author jiangjibo
 * @version 1.0
 * @since 2025/11/3
 */
public class OpenTelemetryIdExtractor implements IdGenerator<String> {

    public static final OpenTelemetryIdExtractor INSTANCE = new OpenTelemetryIdExtractor();

    private int order;

    private Method optlCurrentContextMethod;

    private Class sdkSpanClass;

    public OpenTelemetryIdExtractor() {
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
                throw new IllegalStateException(e);
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
                throw new IllegalStateException("Woody: Failed to get spanContext from optl span!");
            }
            return ReflectionUtils.invoke(spanContext, "getTraceId");
        } catch (Exception e) {
            WoodyLog.error(e, "Woody: Failed to get trace id from optl context!");
            throw new IllegalStateException(e);
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
