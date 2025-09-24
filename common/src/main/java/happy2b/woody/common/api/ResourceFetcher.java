package happy2b.woody.common.api;


import happy2b.woody.common.constant.ProfilingResourceType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public interface ResourceFetcher {

    Map<String, String> resources = new ConcurrentHashMap<>();
    Map<String, AtomicInteger> resourceCounts = new ConcurrentHashMap<>();

    String[] getResourceClassName();

    void fetchResources(Class clazz);

    ProfilingResourceType resourceType();

    boolean isSupport(Class clazz);

}
