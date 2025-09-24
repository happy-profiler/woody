package happy2b.woody.core.manager;

import happy2b.woody.common.id.IdGenerator;
import happy2b.woody.common.id.ThreadLocalRandomIdGenerator;
import happy2b.woody.common.utils.WoodyLog;
import happy2b.woody.common.constant.ProfilingResourceType;
import happy2b.woody.common.dto.ProfilingSpan;
import happy2b.woody.common.dto.ProfilingTrace;
import happy2b.woody.common.dto.ProfilingTraces;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.woody.SpyAPI;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/31
 */
public class TraceManager {

    static volatile TracingState profiling_status = TracingState.OFF_TRACING;

    public static final Map<Long, ProfilingTraces> PROFILING_TRACES = new ConcurrentHashMap<>();

    private static final Map<String, String> RESOURCE_TYPE_THREAD_GROUPS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> THREAD_GROUP_RESOURCE_STACK_FRAME_HEIGHT_MAP = new ConcurrentHashMap<>();

    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

    public static void prepareTracing() {
        profiling_status = TracingState.PREPARING;
    }

    public static void startTracing() {
        profiling_status = TracingState.ON_TRACING;
    }

    public static void stopTracing() {
        profiling_status = TracingState.OFF_TRACING;
    }

    public static SpyAPI.ITrace startProfilingTrace(Long tid, String resource, String type, String methodPath, Object traceId) {
        if (profiling_status == TracingState.ON_TRACING && ResourceMethodManager.INSTANCE.selectedResourceTypes.contains(type)) {
            if (traceId == null) {
                traceId = ThreadLocalRandomIdGenerator.INSTANCE.generateTraceId();
            }
            ProfilingTrace profilingTrace = new ProfilingTrace(resource, type, methodPath, traceId);
            PROFILING_TRACES.computeIfAbsent(tid, thread1 -> new ProfilingTraces()).startProfilingTrace(profilingTrace);
            return profilingTrace;
        }
        if (profiling_status == TracingState.PREPARING && !RESOURCE_TYPE_THREAD_GROUPS.containsKey(type)) {
            processThreadGroupName(type, tid, methodPath);
        }
        return SpyAPI.NO_OP_TRACE;
    }

    public static ProfilingSpan startProfilingSpan(Long tid, Object spanId, long time, String operationName) {
        if (!PROFILING_TRACES.containsKey(tid)) {
            return null;
        }
        return PROFILING_TRACES.get(tid).startProfilingSpan(spanId, time, operationName);
    }

    static Map<Long, Integer> collectResourceThreadIdAndStackFrameHeight(ProfilingResourceType... types) throws InterruptedException {
        int i = 0;
        while (RESOURCE_TYPE_THREAD_GROUPS.size() < types.length) {
            if (i++ > 30) {
                break;
            }
            Thread.sleep(1 * 1000);
        }
        if (RESOURCE_TYPE_THREAD_GROUPS.isEmpty()) {
            throw new IllegalStateException("failed to find resource threads:" + Arrays.stream(types).map(ProfilingResourceType::getValue).collect(Collectors.joining(",")));
        }
        if (RESOURCE_TYPE_THREAD_GROUPS.size() < types.length) {
            for (ProfilingResourceType type : types) {
                if (!RESOURCE_TYPE_THREAD_GROUPS.containsKey(type.getValue())) {
                    WoodyLog.error("Failed to find resource type '{}' threads!", type.getValue());
                }
            }
        }
        Map<Long, Integer> tidHeightMap = new HashMap<>(128);
        for (ThreadInfo threadInfo : THREAD_MX_BEAN.getThreadInfo(THREAD_MX_BEAN.getAllThreadIds())) {
            String threadGroupName = extractThreadGroupName(threadInfo.getThreadName());
            Integer height = THREAD_GROUP_RESOURCE_STACK_FRAME_HEIGHT_MAP.get(threadGroupName);
            if (height != null) {
                tidHeightMap.put(threadInfo.getThreadId(), height);
            }
        }
        return tidHeightMap;
    }

    private static void processThreadGroupName(String type, Long tid, String methodPath) {
        ThreadInfo threadInfo = THREAD_MX_BEAN.getThreadInfo(tid, Integer.MAX_VALUE);
        String threadGroupName = extractThreadGroupName(threadInfo.getThreadName());

        RESOURCE_TYPE_THREAD_GROUPS.put(type, threadGroupName);

        StackTraceElement[] stackTrace = threadInfo.getStackTrace();
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();
            String methodName = element.getMethodName();
            if (methodPath.startsWith(className) && methodPath.endsWith(methodName)) {
                THREAD_GROUP_RESOURCE_STACK_FRAME_HEIGHT_MAP.put(threadGroupName, stackTrace.length - i);
                break;
            }
        }
    }

    private static String extractThreadGroupName(String threadName) {
        int end = -1;
        for (int i = threadName.length() - 1; i >= 0; i--) {
            char c = threadName.charAt(i);
            if (c >= '0' && c <= '9') {
                end = i;
                if (i > 1 && threadName.charAt(i - 1) == '-') {
                    end = i - 1;
                    break;
                }
            } else {
                break;
            }
        }
        if (end == -1) {
            return threadName;
        }
        return threadName.substring(0, end).trim();
    }

    public static void destroy() {
        PROFILING_TRACES.clear();
        RESOURCE_TYPE_THREAD_GROUPS.clear();
        THREAD_GROUP_RESOURCE_STACK_FRAME_HEIGHT_MAP.clear();
    }

    private enum TracingState {
        PREPARING, ON_TRACING, OFF_TRACING
    }

}
