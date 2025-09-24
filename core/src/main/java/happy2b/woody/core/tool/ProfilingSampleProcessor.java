package happy2b.woody.core.tool;

import happy2b.woody.common.dto.ProfilingSample;
import happy2b.woody.common.dto.ProfilingSampleBase;
import happy2b.woody.common.dto.ProfilingTrace;
import happy2b.woody.common.dto.ProfilingTraces;
import happy2b.woody.core.manager.ResourceMethodManager;

import java.util.*;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/14
 */
public class ProfilingSampleProcessor {

    private static final String FRAME_TYPE_ID_LINE_PREFIX = String.format("[%s=", "frame_type_ids");
    private static final String SAMPLE_TIME_LINE_PREFIX = String.format("[%s=", "time");
    private static final String THREAD_ID_LINE_PREFIX = String.format("[%s=", "tid");
    private static final String ALLOC_INSTANCE_LINE_PREFIX = String.format("[%s=", "alloc_instance");
    private static final String RE_FRAME_INDEX_LINE_PREFIX = String.format("[%s=", "rs_frame_index");
    private static final String EVENT_TYPE_LINE_PREFIX = String.format("[%s=", "event_type");


    public static List<ProfilingSample> parseProfilingSamples(String[] lines) {
        ProfilingSample ps = null;
        List<ProfilingSample> result = new ArrayList<>(1024);
        Map<String, String> resourceTypeMappings = ResourceMethodManager.INSTANCE.buildResourceTypeMappings();
        Map<String, String> methodPathResourceMappings = ResourceMethodManager.INSTANCE.buildMethodPathResourceMappings();

        for (int i = 3; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                continue;
            }
            if (line.charAt(0) == ' ') {
                line = line.trim();
            }
            // 样本摘要
            if (line.startsWith("---")) {
                // 计算出现次数
                ps = new ProfilingSample();
                result.add(ps);
                String ticks = line.substring(line.indexOf(",") + 2, line.lastIndexOf("sample") - 1);
                ps.setTicks(Integer.parseInt(ticks));
                continue;
            }
            if (line.charAt(0) == '[') {
                int end = line.length() - 1;
                // 线程id
                if (line.startsWith(THREAD_ID_LINE_PREFIX)) {
                    String tid = line.substring(THREAD_ID_LINE_PREFIX.length(), end);
                    ps.setTid(Long.parseLong(tid));
                }
                // 内存分配
                else if (line.startsWith(ALLOC_INSTANCE_LINE_PREFIX)) {
                    int instanceAlloc = Integer.parseInt(line.substring(ALLOC_INSTANCE_LINE_PREFIX.length(), end));
                    ps.setInstanceAlloc(instanceAlloc);
                }
                // 时间戳
                else if (line.startsWith(SAMPLE_TIME_LINE_PREFIX)) {
                    // 取到时间是微秒格式
                    long time = Long.parseLong((line.substring(SAMPLE_TIME_LINE_PREFIX.length(), end)));
                    ps.setSampleTime(time * 1000);
                }
                // 事件类型
                else if (line.startsWith(EVENT_TYPE_LINE_PREFIX)) {
                    String eventType = line.substring(EVENT_TYPE_LINE_PREFIX.length(), end);
                    ps.setEventType(eventType);
                }
                // 代码栈类型
                else if (line.startsWith(FRAME_TYPE_ID_LINE_PREFIX)) {
                    ps.setFrameTypeIds(line.substring(FRAME_TYPE_ID_LINE_PREFIX.length(), end));
                }
                // 业务入口
                else if (line.startsWith(RE_FRAME_INDEX_LINE_PREFIX)) {
                    int max = ps.getStackTraces().size();
                    int rsHitIndex = Integer.parseInt(line.substring(RE_FRAME_INDEX_LINE_PREFIX.length(), end));
                    rsHitIndex = rsHitIndex < max ? rsHitIndex : max - 1;
                    String methodPath = ps.getStackTraces().get(rsHitIndex);
                    ps.setRsFlagIndex(rsHitIndex);
                    ps.setRsType(resourceTypeMappings.get(methodPath));
                    ps.setResource(methodPathResourceMappings.get(methodPath));
                }
            } else if (ps != null) {
                ps.adStackTrace(line);
            }
        }
        return result;
    }

    public static Map<String, ProfilingSampleBase> extractSampleBase(List<ProfilingSample> samples) {

        Map<String, ProfilingSampleBase> sampleBaseMap = new HashMap<>();

        for (ProfilingSample sample : samples) {
            String resource = sample.getResource();
            if (resource == null) {
                continue;
            }
            Integer rsFlagIndex = sample.getRsFlagIndex();
            int size = sample.getStackTraces().size();
            if (rsFlagIndex == size - 1) {
                continue;
            }

            // 补齐 java.lang.Thread.run
            String mayeLossFrame = "java.lang.Thread.run";
            if (!sample.getStackTraces().get(size - 1).equals(mayeLossFrame)) {
                sample.getStackTraces().add(mayeLossFrame);
                sample.setFrameTypeIds(sample.getFrameTypeIds() + "1");
            }

            List<String> stackFrames = sample.getStackTraces().subList(rsFlagIndex + 1, size);
            ProfilingSampleBase sampleBase = new ProfilingSampleBase();
            sampleBase.setResource(resource);
            sampleBase.setRsType(sample.getRsType());
            String baseFrameTypeIds = sample.getFrameTypeIds().substring(rsFlagIndex + 1);
            sampleBase.setFrameTypeIds(baseFrameTypeIds);
            sampleBase.setStackTraces(stackFrames);
            sampleBase.setRsType(sample.getRsType());
            sampleBaseMap.put(resource, sampleBase);

            String subRFI = sample.getFrameTypeIds().substring(0, rsFlagIndex + 1);
            sample.setFrameTypeIds(subRFI);
            List<String> subStackTraces = sample.getStackTraces().subList(0, rsFlagIndex + 1);
            sample.setStackTraces(subStackTraces);
        }
        return sampleBaseMap;
    }


    public static void populateTracingInfo(List<ProfilingSample> samples, Map<Long, ProfilingTraces> traceIdBuffer) {
        Iterator<ProfilingSample> iterator = samples.iterator();
        while (iterator.hasNext()) {
            ProfilingSample sample = iterator.next();
            if (sample.getResource() == null) {
                iterator.remove();
                continue;
            }
            ProfilingTraces traces = traceIdBuffer.get(sample.getTid());
            if (traces == null || traces.getTraceList().isEmpty()) {
                continue;
            }
            ProfilingTrace trace = traces.findTraceByTime(sample.getSampleTime(), sample.getResource());
            if (trace != null) {
                sample.setTraceId(trace.getTraceId());
            }
        }
    }

}
