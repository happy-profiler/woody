package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.core.flame.common.constant.ProfilingEvent;
import happy2b.woody.core.flame.common.dto.ProfilingSample;
import happy2b.woody.core.flame.common.dto.ProfilingSampleBase;
import happy2b.woody.core.flame.common.dto.TraceSamples;
import happy2b.woody.core.flame.manager.ProfilingManager;
import happy2b.woody.core.server.WoodyBootstrap;
import happy2b.woody.core.tool.graph.FlameGraph;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jiangjibo
 * @version 1.0
 * @description: trace sample
 * -l list
 * -f flame graph file
 * -c clear
 * --file fileName
 * --event value
 * --id traceId
 * --top N
 * @since 2025/8/25
 */
public class TSCommandExecutor implements WoodyCommandExecutor {

    // 定义格式化模板（线程安全，可全局复用）
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    TSCommandExecutor() {
    }

    @Override
    public String commandName() {
        return "ts";
    }

    @Override
    public boolean support(WoodyCommand command) {
        return command.getEval().startsWith(commandName() + " ");
    }

    @Override
    public void executeInternal(WoodyCommand command) throws IOException {
        String[] segments = splitCommandEval(command);

        int topN = 0;
        int opCount = 0;
        String file = null, id = null;
        ProfilingEvent event = null;
        boolean list = false, fg = false, clear = false;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (segment.equals(commandName())) {

            } else if (segment.equals("-l")) {
                list = true;
                opCount++;
            } else if (segment.equals("-f")) {
                fg = true;
                opCount++;
            } else if (segment.equals("--file")) {
                if (i == segments.length - 1) {
                    command.error("missing trace sample file param value!");
                    return;
                }
                file = segments[++i].trim();
            } else if (segment.equals("-c")) {
                clear = true;
                opCount++;
            } else if (segment.equals("--id")) {
                if (i == segments.length - 1) {
                    command.error("missing trace sample id param value!");
                    return;
                }
                id = segments[++i].trim();
            } else if (segment.equals("--top")) {
                if (i == segments.length - 1) {
                    command.error("missing trace sample topN param value!");
                    return;
                }
                topN = Integer.parseInt(segments[++i].trim());
            } else if (segment.equals("--event")) {
                if (i == segments.length - 1) {
                    command.error("missing trace sample event param value!");
                    return;
                }
                event = ProfilingEvent.ofValue(segments[++i].trim());
                if (event == null) {
                    command.error("invalid trace sample event param value: " + segments[i]);
                    return;
                }
            } else {
                command.error("invalid profiling command!");
                return;
            }
        }
        if (opCount != 1) {
            command.error("invalid trace sample command!");
            return;
        }
        if (topN > 100) {
            command.error("topN must less than 100");
            return;
        }
        if (topN > 0 && id != null) {
            command.error("topN and ids can not be set at the same time");
            return;
        }
        if (fg && file == null) {
            command.error("file must be set when generating flame graph");
            return;
        }
        if (!fg && file != null) {
            command.error("flame graph file only can be set when generating flame graph");
            return;
        }
        TraceSamples traceSamples = ProfilingManager.INSTANCE.getTraceSamples();
        if (traceSamples == null) {
            command.error("no profiling record stored!");
            return;
        }
        boolean multiEvent = traceSamples.getEventSamples().size() > 1;
        if (multiEvent && event == null) {
            command.error("profiling event can not be null when process multi event trace samples!");
            return;
        }
        if (event == null) {
            event = ProfilingEvent.ofValue(traceSamples.getEventSamples().keySet().iterator().next());
        }
        if (clear) {
            ProfilingManager.INSTANCE.setTraceSamples(null);
            command.result("clear trace samples success!");
            return;
        }
        if (list) {
            listTraceSample(command, event, topN, id);
            return;
        }
        if (fg) {
            generateFlameGraph(command, event, file, topN, id);
        }
    }

    private void listTraceSample(WoodyCommand command, ProfilingEvent event, int topN, String id) {
        if (id != null) {
            List<ProfilingSample> targetSamples = getTraceSampleById(event, id);
            if (targetSamples == null) {
                return;
            }
            if (targetSamples.isEmpty()) {
                command.result("[]");
            } else {
                command.result(formatSamples(targetSamples));
            }
        } else {
            List<List<ProfilingSample>> topNTraceSamples = getTopNTraceSamples(event, topN);
            String result = formatTopNTraceSamples(topNTraceSamples, event);
            command.result(result);
        }
    }

    private List<ProfilingSample> getTraceSampleById(ProfilingEvent event, String id) {
        Map<String, List<ProfilingSample>> eventSamples = ProfilingManager.INSTANCE.getTraceSamples().getEventSamples();
        List<ProfilingSample> samples;
        samples = eventSamples.get(event.getSegment());
        Object typeMatchId = convertId(id, samples);
        return samples.stream().filter(profilingSample -> typeMatchId.equals(profilingSample.getTraceId())).collect(Collectors.toList());
    }

    private Object convertId(String id, List<ProfilingSample> samples) {
        Optional<ProfilingSample> optional = samples.stream().filter(profilingSample -> profilingSample.getTraceId() != null).findFirst();
        if (!optional.isPresent()) {
            return id;
        }
        Object traceId = optional.get().getTraceId();
        try {
            if (traceId instanceof Long) {
                return Long.parseLong(id);
            } else if (traceId instanceof Integer) {
                return Integer.parseInt(id);
            } else {
                return id;
            }
        } catch (Exception e) {
            throw new IllegalStateException("can`t convert string trace id to target type");
        }
    }

    private List<List<ProfilingSample>> getTopNTraceSamples(ProfilingEvent event, int topN) {
        List<ProfilingSample> samples = ProfilingManager.INSTANCE.getTraceSamples().getEventSamples().get(event.getSegment());
        if (samples == null || samples.isEmpty()) {
            return null;
        }
        Map<Object, List<ProfilingSample>> groupedSamples = samples.stream().filter(profilingSample -> profilingSample.getTraceId() != null).collect(Collectors.groupingBy(profilingSample -> profilingSample.getTraceId()));
        List<List<ProfilingSample>> flatSamples = new ArrayList<>(groupedSamples.values());
        sortProfilingSamples(flatSamples);
        List<List<ProfilingSample>> lists = flatSamples.subList(0, topN);
        return lists;
    }

    private String formatSamples(List<ProfilingSample> samples) {
        samples = samples.stream().sorted((o1, o2) -> o1.getSampleTime() > o2.getSampleTime() ? 1 : -1).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder("\n");
        ProfilingSample sample = samples.get(0);
        String eventType = sample.getEventType();
        sb.append("eventType:").append(eventType).append(", traceId:").append(sample.getTraceId()).append("\n");
        for (ProfilingSample sp : samples) {
            Instant instant = Instant.ofEpochMilli(sp.getSampleTime() / 1_000_000);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            sb.append("\n").append("time:").append(localDateTime.format(formatter));
            if (ProfilingEvent.ALLOC.getSegment().equals(eventType)) {
                sb.append(",alloc:").append(sp.getInstanceAlloc());
            }
            for (String stackTrace : sp.getStackTraces()) {
                sb.append("\n").append(stackTrace);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private void sortProfilingSamples(List<List<ProfilingSample>> samples) {
        Comparator<List<ProfilingSample>> comparator = (o1, o2) -> {
            String eventType = o1.get(0).getEventType();
            switch (eventType) {
                case "cpu":
                case "wall":
                case "lock":
                    int t1 = o1.size();
                    int t2 = o2.size();
                    if (t1 != t2) {
                        return t2 - t1;
                    }
                    return (int) (o2.get(0).getSampleTime() - o1.get(0).getSampleTime());
                case "alloc":
                    int a1 = sumAlloc(o1);
                    int a2 = sumAlloc(o2);
                    if (a1 != a2) {
                        return a2 - a1;
                    }
                    return (int) (o2.get(0).getSampleTime() - o1.get(0).getSampleTime());
                default:
                    throw new IllegalStateException("Should never get here");
            }
        };
        Collections.sort(samples, comparator);
    }

    private int sumAlloc(List<ProfilingSample> samples) {
        int allocByte = 0;
        for (ProfilingSample sample : samples) {
            allocByte += sample.getInstanceAlloc() * sample.getTicks();
        }
        return allocByte;
    }

    private void generateFlameGraph(WoodyCommand command, ProfilingEvent event, String file, int topN, String id) throws IOException {
        if (id != null) {
            List<ProfilingSample> targetSamples = getTraceSampleById(event, id);
            if (targetSamples == null) {
                return;
            }
            if (targetSamples.isEmpty()) {
                command.error("no sample found for trace id: " + id);
                return;
            }
            generateFlameGraph(targetSamples, file);
        } else if (topN > 0) {
            List<List<ProfilingSample>> samples = getTopNTraceSamples(event, topN);
            if (samples == null || samples.size() == 0) {
                command.result("no trace samples");
                return;
            }
            List<ProfilingSample> targetSamples = new ArrayList<>();
            samples.stream().forEach(profilingSamples -> targetSamples.addAll(profilingSamples));
            generateFlameGraph(targetSamples, file);
        } else {
            if (event != null) {
                List<ProfilingSample> targetSamples = ProfilingManager.INSTANCE.getTraceSamples().getEventSamples().get(event.getSegment());
                if (targetSamples.isEmpty() || targetSamples.isEmpty()) {
                    command.result("no trace samples");
                    return;
                }
                generateFlameGraph(targetSamples, file);
            } else {
                for (Map.Entry<String, List<ProfilingSample>> entry : ProfilingManager.INSTANCE.getTraceSamples().getEventSamples().entrySet()) {
                    List<ProfilingSample> targetSamples = entry.getValue();
                    generateFlameGraph(targetSamples, file);
                }
            }
        }
        command.result("flame graph generate success!");
    }

    private void generateFlameGraph(List<ProfilingSample> targetSamples, String file) throws IOException {
        if (!file.endsWith(".html")) {
            file = file + ".html";
        }
        Map<String, ProfilingSampleBase> sampleBaseMap = ProfilingManager.INSTANCE.getTraceSamples().getSampleBaseMap();
        String parent = WoodyBootstrap.getInstance().getWoodyHome() + File.separator;
        String outputFile = parent + file;
        File targetFile = new File(outputFile);
        if (targetFile.exists()) {
            targetFile.delete();
        }
        FlameGraph.convert(targetSamples, sampleBaseMap, outputFile);
    }

    private String formatTopNTraceSamples(List<List<ProfilingSample>> samplesList, ProfilingEvent eventType) {
        StringBuilder sb = new StringBuilder();
        for (List<ProfilingSample> samples : samplesList) {
            samples = samples.stream().sorted(Comparator.comparingLong(ProfilingSample::getSampleTime)).collect(Collectors.toList());

            Instant instant = Instant.ofEpochMilli(samples.get(0).getSampleTime() / 1_000_000);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            String startTime = localDateTime.format(formatter);

            instant = Instant.ofEpochMilli(samples.get(samples.size() - 1).getSampleTime() / 1_000_000);
            localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            String endTime = localDateTime.format(formatter);

            sb.append("event:").append(eventType.getSegment()).append(", traceId:").append(samples.get(0).getTraceId())
                    .append(", sampleNum:").append(samples.size()).append(", start:").append(startTime).append(", end:").append(endTime);

            if (eventType == ProfilingEvent.ALLOC) {
                int allocSum = samples.stream().mapToInt(ProfilingSample::getInstanceAlloc).sum();
                sb.append(", alloc:").append(allocSum);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
