package happy2b.woody.core.transform;

import happy2b.woody.common.id.IdGenerator;
import happy2b.woody.common.id.ParametricIdGenerator;
import happy2b.woody.common.reflection.ReflectionUtils;
import happy2b.woody.core.manager.ResourceMethodManager;
import happy2b.woody.core.manager.TraceManager;
import happy2b.woody.common.api.ResourceMethod;

import java.woody.SpyAPI;
import java.lang.reflect.Method;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/24
 */
public class ResourceMethodAdvice extends SpyAPI.AbstractSpy {

    public static final String ADVICE_CLASS = SpyAPI.class.getName().replace(".", "/");
    public static final String PROFILING_TRACE_CLASS = SpyAPI.ITrace.class.getName().replace(".", "/");
    public static final String PROFILING_TRACE_CLASS_DESC = "L" + PROFILING_TRACE_CLASS + ";";

    public static Method START_TRACE_METHOD;
    public static Method START_TRACE_WITH_PARAM_METHOD;
    public static Method START_SPAN_METHOD;
    public static Method START_SPAN_WITH_PARAM_METHOD;
    public static Method FINISH_TRACE_METHOD;

    private static SpyAPI.AbstractSpy spy = new ResourceMethodAdvice();

    static {
        START_TRACE_METHOD = ReflectionUtils.findMethod(SpyAPI.class, "startTrace", String.class, String.class, String.class, int.class);
        START_TRACE_WITH_PARAM_METHOD = ReflectionUtils.findMethod(SpyAPI.class, "startTrace", String.class, String.class, String.class, int.class, Object.class);
        START_SPAN_METHOD = ReflectionUtils.findMethod(SpyAPI.class, "startSpan", String.class, String.class, int.class);
        START_SPAN_WITH_PARAM_METHOD = ReflectionUtils.findMethod(SpyAPI.class, "startSpan", String.class, String.class, int.class, Object.class);
        FINISH_TRACE_METHOD = ReflectionUtils.findMethod(SpyAPI.ITrace.class, "finish");

        SpyAPI.setSpy(spy);
    }

    public SpyAPI.ITrace startTrace(String resourceType, String resource, String methodPath, int generatorIndex) {
        return TraceManager.startProfilingTrace(Thread.currentThread().getId(), resource, resourceType, methodPath, IdGenerator.ID_GENERATORS[generatorIndex].generateTraceId());
    }

    public SpyAPI.ITrace startTrace(String resourceType, String resource, String methodPath, int generatorIndex, Object target) {
        ParametricIdGenerator idGenerator = (ParametricIdGenerator) IdGenerator.ID_GENERATORS[generatorIndex];
        ResourceMethod resourceMethod = ResourceMethodManager.INSTANCE.findResourceMethodByMethodPath(methodPath);
        return TraceManager.startProfilingTrace(Thread.currentThread().getId(), resource, resourceType, methodPath, idGenerator.generateTraceId(target, resourceMethod.getFunctionTokenExecutors()));
    }

    public SpyAPI.ISpan startSpan(String operationName, String methodPath, int generatorIndex) {
        return TraceManager.startProfilingSpan(Thread.currentThread().getId(), IdGenerator.ID_GENERATORS[generatorIndex].generateSpanId(), System.nanoTime(), operationName);
    }

    public SpyAPI.ISpan startSpan(String operationName, String methodPath, int generatorIndex, Object param) {
        ParametricIdGenerator idGenerator = (ParametricIdGenerator) IdGenerator.ID_GENERATORS[generatorIndex];
        ResourceMethod resourceMethod = ResourceMethodManager.INSTANCE.findResourceMethodByMethodPath(methodPath);
        return TraceManager.startProfilingSpan(Thread.currentThread().getId(), idGenerator.generateSpanId(param, resourceMethod.getFunctionTokenExecutors()), System.nanoTime(), operationName);
    }

    public static void destroy() {
        START_TRACE_METHOD = null;
        START_TRACE_WITH_PARAM_METHOD = null;
        START_SPAN_METHOD = null;
        START_SPAN_WITH_PARAM_METHOD = null;
        FINISH_TRACE_METHOD = null;
    }

}
