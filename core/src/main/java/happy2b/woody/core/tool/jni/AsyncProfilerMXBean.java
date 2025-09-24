/*
 * Copyright The async-profiler authors
 * SPDX-License-Identifier: Apache-2.0
 */

package happy2b.woody.core.tool.jni;

import happy2b.woody.common.api.ResourceMethod;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AsyncProfiler interface for JMX server.
 * How to register AsyncProfiler MBean:
 *
 * <pre>{@code
 *     ManagementFactory.getPlatformMBeanServer().registerMBean(
 *             AsyncProfiler.getInstance(),
 *             new ObjectName("one.profiler:type=AsyncProfiler")
 *     );
 * }</pre>
 */
public interface AsyncProfilerMXBean {
    void start(Map<String, Long> eventIntervals) throws IllegalStateException;

    void resume(Map<String, Long> eventIntervals) throws IllegalStateException;

    void stop() throws IllegalStateException;

    long getSamples();

    String dumpTraces(int maxTraces);

    String getVersion();

    Set<String> getSupportEvents();

    void setResourceMethods(List<ResourceMethod> methods) throws IllegalStateException;

    <T> T[] getInstances(Class<T> clazz, int limit) throws IllegalStateException;

    void syncTidRsStackFrameHeightMap(Map<Long, Integer> heightMap);
}
