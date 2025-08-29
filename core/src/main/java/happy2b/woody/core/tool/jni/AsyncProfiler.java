/*
 * Copyright The async-profiler authors
 * SPDX-License-Identifier: Apache-2.0
 */

package happy2b.woody.core.tool.jni;

import happy2b.woody.core.flame.resource.ResourceMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java API for in-process profiling. Serves as a wrapper around
 * async-profiler native library. This class is a singleton.
 * The first call to {@link #getInstance()} initiates loading of
 * libasyncProfiler.so.
 */
public class AsyncProfiler implements AsyncProfilerMXBean {
    private static AsyncProfiler instance;

    private AsyncProfiler() {
    }

    public static AsyncProfiler getInstance() {
        return getInstance(null);
    }

    public static synchronized AsyncProfiler getInstance(String libPath) {
        if (instance != null) {
            return instance;
        }

        AsyncProfiler profiler = new AsyncProfiler();
        if (libPath != null) {
            System.load(libPath);
        } else {
            try {
                // No need to load library, if it has been preloaded with -agentpath
                profiler.getVersion();
            } catch (UnsatisfiedLinkError e) {
                File file = extractEmbeddedLib();
                if (file != null) {
                    try {
                        System.load(file.getPath());
                    } finally {
                        file.delete();
                    }
                } else {
                    System.loadLibrary("asyncProfiler");
                }
            }
        }

        instance = profiler;
        return profiler;
    }

    private static File extractEmbeddedLib() {
        String platformTag = getPlatformTag();
        String resourceName, suffix;
        if (platformTag.contains("x64")) {
            resourceName = "/x64/libasyncProfiler.so";
            suffix = ".so";
        } else if (platformTag.contains("arm64")) {
            resourceName = "/arm64/libasyncProfiler.so";
            suffix = ".so";
        } else if (platformTag.contains("mac")) {
            resourceName = "/mac/libasyncProfiler.dylib";
            suffix = ".dylib";
        } else {
            throw new IllegalStateException("Unsupported platform: " + platformTag);
        }
        InputStream in = AsyncProfiler.class.getResourceAsStream(resourceName);
        if (in == null) {
            return null;
        }

        try {
            File file = File.createTempFile("libasyncProfiler-.", suffix);
            try (FileOutputStream out = new FileOutputStream(file)) {
                byte[] buf = new byte[32000];
                for (int bytes; (bytes = in.read(buf)) >= 0; ) {
                    out.write(buf, 0, bytes);
                }
            }
            return file;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static String getPlatformTag() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        if (os.contains("linux")) {
            if (arch.equals("amd64") || arch.equals("x86_64") || arch.contains("x64")) {
                return "linux-x64";
            } else if (arch.equals("aarch64") || arch.contains("arm64")) {
                return "linux-arm64";
            } else if (arch.equals("aarch32") || arch.contains("arm")) {
                return "linux-arm32";
            } else if (arch.contains("86")) {
                return "linux-x86";
            } else if (arch.contains("ppc64")) {
                return "linux-ppc64le";
            }
        } else if (os.contains("mac")) {
            return "macos";
        }
        throw new UnsupportedOperationException("Unsupported platform: " + os + "-" + arch);
    }

    /**
     * Start profiling
     *
     * @param eventIntervals#key   Profiling event, see {@link Events}
     * @param eventIntervals#value Sampling interval, e.g. nanoseconds for  {@link Events#CPU}, {@link Events#WALL}, {@link Events#LOCK}, kb for  {@link Events#ALLOC}
     * @throws IllegalStateException If profiler is already running
     */
    @Override
    public void start(Map<String, Long> eventIntervals) throws IllegalStateException {
        if (eventIntervals == null || eventIntervals.isEmpty()) {
            throw new NullPointerException();
        }
        startInternal(eventIntervals, true);
    }

    /**
     * Start or resume profiling without resetting collected data.
     * Note that event and interval may change since the previous profiling session.
     *
     * @param eventIntervals#key   Profiling event, see {@link Events}
     * @param eventIntervals#value Sampling interval, e.g. nanoseconds for  {@link Events#CPU}, {@link Events#WALL}, {@link Events#LOCK}, kb for  {@link Events#ALLOC}
     * @throws IllegalStateException If profiler is already running
     */
    @Override
    public void resume(Map<String, Long> eventIntervals) throws IllegalStateException {
        if (eventIntervals == null || eventIntervals.isEmpty()) {
            throw new NullPointerException();
        }
        startInternal(eventIntervals, false);
    }

    private void startInternal(Map<String, Long> eventIntervals, boolean reset) throws IllegalStateException {
        long defaultInterval = -1L;
        long cpuInterval = eventIntervals.getOrDefault(Events.CPU, defaultInterval);
        if (cpuInterval == defaultInterval) {
            cpuInterval = eventIntervals.getOrDefault(Events.CTIMER, defaultInterval);
        }
        if (cpuInterval == defaultInterval) {
            cpuInterval = eventIntervals.getOrDefault(Events.ITIMER, defaultInterval);
        }
        long wallInterval = eventIntervals.getOrDefault(Events.WALL, defaultInterval);
        long lockInterval = eventIntervals.getOrDefault(Events.LOCK, defaultInterval);
        long allocInterval = eventIntervals.getOrDefault(Events.ALLOC, defaultInterval);

        start0(cpuInterval, wallInterval, lockInterval, allocInterval, reset);
    }

    /**
     * Stop profiling (without dumping results)
     *
     * @throws IllegalStateException If profiler is not running
     */
    @Override
    public void stop() throws IllegalStateException {
        stop0();
    }

    /**
     * Get the number of samples collected during the profiling session
     *
     * @return Number of samples
     */
    @Override
    public native long getSamples();

    /**
     * Get profiler agent version, e.g. "1.0"
     *
     * @return Version string
     */
    @Override
    public String getVersion() {
        try {
            return execute0("version");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Dump collected stack traces
     *
     * @param maxTraces Maximum number of stack traces to dump. 0 means no limit
     * @return Textual representation of the profile
     */
    @Override
    public String dumpTraces(int maxTraces) {
        try {
            return execute0(maxTraces == 0 ? "traces" : "traces=" + maxTraces);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Set<String> getSupportEvents() {
        return getSupportEvents0();
    }

    public void setResourceMethods(List<ResourceMethod> methods) {
        setResourceMethods0(methods.toArray(new ResourceMethod[0]));
    }

    @Override
    public <T> T[] getInstances(Class<T> clazz, int limit) {
        return (T[]) getInstances0(clazz, limit);
    }

    @Override
    public void syncTidRsStackFrameHeightMap(Map<Long, Integer> heightMap) {
        if (heightMap == null || heightMap.isEmpty()) {
            throw new NullPointerException();
        }
        syncTidRsStackFrameHeightMap0(heightMap);
    }

    public void destroy() {
        instance = null;
    }

    private native Set<String> getSupportEvents0();

    private native void start0(long cpuInterval, long wallInterval, long lockInterval, long allocInterval, boolean reset) throws IllegalStateException;

    private native void stop0() throws IllegalStateException;

    private native String execute0(String command) throws IllegalArgumentException, IllegalStateException, IOException;

    private native Object[] getInstances0(Class clazz, int limit) throws IllegalStateException;

    private native void setResourceMethods0(ResourceMethod[] methods);

    private native void syncTidRsStackFrameHeightMap0(Map<Long, Integer> heightMap);


}
