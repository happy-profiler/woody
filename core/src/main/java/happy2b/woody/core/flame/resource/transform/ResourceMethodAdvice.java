package happy2b.woody.core.flame.resource.transform;

import happy2b.woody.common.api.id.ParametricIdGenerator;
import happy2b.woody.common.reflection.ReflectionUtils;
import happy2b.woody.core.flame.core.ResourceMethodManager;
import happy2b.woody.core.flame.core.TraceManager;

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

    public static final Method START_TRACE_METHOD;
    public static final Method START_TRACE_WITH_PARAM_METHOD;
    public static final Method START_SPAN_METHOD;
    public static final Method START_SPAN_WITH_PARAM_METHOD;
    public static final Method FINISH_TRACE_METHOD;

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
        return TraceManager.startProfilingTrace(Thread.currentThread().getId(), resource, resourceType, methodPath, ResourceMethodManager.INSTANCE.idGenerators[generatorIndex].generateTraceId());
    }

    public SpyAPI.ITrace startTrace(String resourceType, String resource, String methodPath, int generatorIndex, Object param) {
        ParametricIdGenerator idGenerator = (ParametricIdGenerator) ResourceMethodManager.INSTANCE.idGenerators[generatorIndex];
        return TraceManager.startProfilingTrace(Thread.currentThread().getId(), resource, resourceType, methodPath, idGenerator.generateTraceId(param));
    }

    public SpyAPI.ISpan startSpan(String operationName, String methodPath, int generatorIndex) {
        return TraceManager.startProfilingSpan(Thread.currentThread().getId(), ResourceMethodManager.INSTANCE.idGenerators[generatorIndex].generateSpanId(), System.nanoTime(), operationName);
    }

    public SpyAPI.ISpan startSpan(String operationName, String methodPath, int generatorIndex, Object param) {
        ParametricIdGenerator idGenerator = (ParametricIdGenerator) ResourceMethodManager.INSTANCE.idGenerators[generatorIndex];
        return TraceManager.startProfilingSpan(Thread.currentThread().getId(), idGenerator.generateSpanId(param), System.nanoTime(), operationName);
    }


}
