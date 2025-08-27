package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.core.flame.manager.ProfilingManager;
import happy2b.woody.core.tool.jni.AsyncProfiler;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author jiangjibo
 * @version 1.0
 * @description: profiling events[interval] -c(clear) -s(selected) -l(list) --cpu cpuInterval
 * -l(list)
 * -ls(list selected)
 * -c(clear)
 * -s(select)
 * @since 2025/8/22
 */
public class PECommandExecutor implements WoodyCommandExecutor {

    private static final long DEFAULT_CPU_INTERVAL = 5_000_000L; // 5ms
    private static final long DEFAULT_WALL_INTERVAL = 50_000_000L; // 50ms
    private static final long DEFAULT_LOCK_INTERVAL = 50_000_000L; // 50ms
    private static final long DEFAULT_ALLOC_INTERVAL = 1024 * 1024L; // 1024kb

    @Override
    public String commandName() {
        return "pe";
    }

    @Override
    public boolean support(WoodyCommand command) {
        return command.getEval().startsWith(commandName() + " ");
    }

    @Override
    public void executeInternal(WoodyCommand command) {
        String[] segments = splitCommandEval(command);

        int opCount = 0;
        boolean clear = false, list = false, listSelected = false, select = false;
        long cpuInterval = 0, allocInterval = 0, wallInterval = 0, lockInterval = 0;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (segment.equals(commandName())) {
                continue;
            } else if (segment.equals("-c")) {
                if (i != segments.length - 1) {
                    command.error("clear profiling events not support other arguments!");
                    return;
                }
                opCount++;
                clear = true;
            } else if (segment.equals("-ls")) {
                opCount++;
                listSelected = true;
            } else if (segment.equals("-l")) {
                if (i != segments.length - 1) {
                    command.error("list profiling events not support other arguments!");
                    return;
                }
                opCount++;
                list = true;
            } else if (segment.equals("-s")) {
                opCount++;
                select = true;
            } else if (segment.equals("--cpu")) {
                if (i == segments.length - 1) {
                    cpuInterval = DEFAULT_CPU_INTERVAL;
                } else {
                    String nextSeg = segments[i + 1];
                    if (nextSeg.startsWith("-")) {
                        cpuInterval = DEFAULT_CPU_INTERVAL;
                    } else {
                        cpuInterval = Long.parseLong(nextSeg) * 1_000_000L;
                        i++;
                    }
                }
            } else if (segment.equals("--wall")) {
                if (i == segments.length - 1) {
                    wallInterval = DEFAULT_WALL_INTERVAL;
                } else {
                    String nextSeg = segments[i + 1];
                    if (nextSeg.startsWith("-")) {
                        wallInterval = DEFAULT_WALL_INTERVAL;
                    } else {
                        wallInterval = Long.parseLong(nextSeg) * 1_000_000L;
                        i++;
                    }
                }
            } else if (segment.equals("--lock")) {
                if (i == segments.length - 1) {
                    lockInterval = DEFAULT_LOCK_INTERVAL;
                } else {
                    String nextSeg = segments[i + 1];
                    if (nextSeg.startsWith("-")) {
                        lockInterval = DEFAULT_LOCK_INTERVAL;
                    } else {
                        lockInterval = Long.parseLong(nextSeg) * 1_000_000L;
                        i++;
                    }
                }
            } else if (segment.equals("--alloc")) {
                if (i == segments.length - 1) {
                    allocInterval = DEFAULT_ALLOC_INTERVAL;
                } else {
                    String nextSeg = segments[i + 1];
                    if (nextSeg.startsWith("-")) {
                        allocInterval = DEFAULT_ALLOC_INTERVAL;
                    } else {
                        allocInterval = Long.parseLong(nextSeg) * 1024;
                        i++;
                    }
                }
            } else {
                command.error("unknown argument: " + segment);
                return;
            }
        }
        if (opCount != 1) {
            command.error("invalid profiling event command!");
            return;
        }

        if (clear) {
            ProfilingManager.INSTANCE.clearIntervals();
            command.result("clear profiling events success!");
            return;
        }

        if (listSelected) {
            Map<String, Long> intervals = ProfilingManager.INSTANCE.getEventIntervals();
            command.result(formatEventIntervals(intervals));
            return;
        }

        if (list) {
            Set<String> supportEvents = AsyncProfiler.getInstance().getSupportEvents();
            StringBuilder sb = new StringBuilder("[");
            supportEvents.remove("wall");
            sb.append("wall,");
            if (supportEvents.remove("alloc")) {
                sb.append("alloc,");
            }
            if (supportEvents.remove("lock")) {
                sb.append("lock,");
            }
            if (!supportEvents.isEmpty()) {
                String cpuEvents = supportEvents.stream().collect(Collectors.joining(","));
                sb.append("cpu(").append(cpuEvents).append(")]");
            }
            command.result(sb.toString());
            return;
        }

        if (!select) {
            return;
        }

        ProfilingManager.INSTANCE.setEventInterval(cpuInterval, wallInterval, lockInterval, allocInterval);

        command.result("select profiling event success:" + formatEventIntervals(ProfilingManager.INSTANCE.getEventIntervals()));
    }

    private String formatEventIntervals(Map<String, Long> eventIntervals) {
        String result = eventIntervals.entrySet().stream().map(new Function<Map.Entry<String, Long>, String>() {
            @Override
            public String apply(Map.Entry<String, Long> entry) {
                if (entry.getKey().equals("alloc")) {
                    return ("alloc:") + (entry.getValue() / 1024) + ("kb");
                } else {
                    return entry.getKey() + (":") + (entry.getValue() / 1_000_000) + ("ms");
                }
            }
        }).collect(Collectors.joining(","));
        return "[" + result + "]";
    }

}
