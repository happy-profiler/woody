package happy2b.woody.core.flame.manager;


import happy2b.woody.core.flame.common.constant.ProfilingEvent;
import happy2b.woody.core.flame.common.constant.ProfilingResourceType;
import happy2b.woody.core.flame.common.dto.ProfilingSample;
import happy2b.woody.core.flame.common.dto.TraceSamples;
import happy2b.woody.core.flame.resource.ResourceMethod;
import happy2b.woody.core.tool.ProfilingSampleProcessor;
import happy2b.woody.core.tool.jni.AsyncProfiler;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/23
 */
public class ProfilingManager {

    public static ProfilingManager INSTANCE = new ProfilingManager();

    private Map<String, Long> eventIntervals = new HashMap<>();

    private TraceSamples traceSamples;

    private boolean profiling = false;

    private ProfilingManager() {
    }

    public void setEventInterval(long cpuInterval, long wallInterval, long lockInterval, long allocInterval) {
        if (cpuInterval > 0) {
            eventIntervals.put(ProfilingEvent.CPU.getSegment(), cpuInterval);
        }
        if (wallInterval > 0) {
            eventIntervals.put(ProfilingEvent.WALL.getSegment(), wallInterval);
        }
        if (lockInterval > 0) {
            eventIntervals.put(ProfilingEvent.LOCK.getSegment(), lockInterval);
        }
        if (allocInterval > 0) {
            eventIntervals.put(ProfilingEvent.ALLOC.getSegment(), allocInterval);
        }
    }

    public TraceSamples getTraceSamples() {
        return traceSamples;
    }

    public void setTraceSamples(TraceSamples traceSamples) {
        this.traceSamples = traceSamples;
    }

    public Map<String, Long> getEventIntervals() {
        return eventIntervals;
    }

    public void clearIntervals() {
        eventIntervals.clear();
    }

    public void startProfiling() throws Throwable {
        Map<String, Set<ResourceMethod>> resources = ResourceFetcherManager.INSTANCE.listAllSelectedResources();

        Set<Class> resourceClasses = resources.values().stream().flatMap(set -> set.stream()).map(resourceMethod -> resourceMethod.getClazz()).collect(Collectors.toSet());

        ResourceClassManager.INSTANCE.retransformResourceClasses(resourceClasses);


        AsyncProfiler asyncProfiler = AsyncProfiler.getInstance();

        TraceManager.prepareTracing();

        ProfilingResourceType[] resourceTypes = resources.keySet().stream().map(resourceType -> ProfilingResourceType.ofType(resourceType))
                .collect(Collectors.toList()).toArray(new ProfilingResourceType[0]);
        Map<Long, Integer> tidRSFrameHeightMap = TraceManager.collectResourceThreadIdAndStackFrameHeight(resourceTypes);
        if (tidRSFrameHeightMap.isEmpty()) {
            throw new IllegalStateException("No resource thread found");
        }
        asyncProfiler.syncTidRsStackFrameHeightMap(tidRSFrameHeightMap);

        TraceManager.startTracing();

        List<ResourceMethod> methods = new ArrayList<>();
        for (Set<ResourceMethod> value : resources.values()) {
            methods.addAll(value);
        }
        asyncProfiler.setResourceMethods(methods);

        asyncProfiler.start(eventIntervals);

        profiling = true;
    }

    public List<ProfilingSample> stopProfiling() throws Throwable {
        TraceManager.stopTracing();
        AsyncProfiler asyncProfiler = AsyncProfiler.getInstance();
        asyncProfiler.stop();
        String dumpTraces = asyncProfiler.dumpTraces(0);
        String[] split = dumpTraces.split(System.lineSeparator());
        return ProfilingSampleProcessor.parseProfilingSamples(split);
    }

    public static void destroy() {
        if (INSTANCE != null) {
            if (INSTANCE.profiling) {
                try {
                    INSTANCE.stopProfiling();
                } catch (Throwable e) {
                    // ignore
                }
            }
            INSTANCE.traceSamples = null;
            INSTANCE.eventIntervals = null;
            INSTANCE = null;
        }
    }

}
