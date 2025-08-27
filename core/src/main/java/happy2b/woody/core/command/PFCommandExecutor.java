package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.common.thread.AgentThreadFactory;
import happy2b.woody.core.flame.common.dto.ProfilingSample;
import happy2b.woody.core.flame.common.dto.ProfilingSampleBase;
import happy2b.woody.core.flame.common.dto.TraceSamples;
import happy2b.woody.core.flame.manager.ProfilingManager;
import happy2b.woody.core.flame.manager.ResourceFetcherManager;
import happy2b.woody.core.flame.manager.TraceManager;
import happy2b.woody.core.flame.resource.ResourceMethod;
import happy2b.woody.core.server.WoodyBootstrap;
import happy2b.woody.core.server.WoodyServerHandler;
import happy2b.woody.core.tool.ProfilingSampleProcessor;
import happy2b.woody.core.tool.graph.FlameGraph;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author jiangjibo
 * @version 1.0 2025/8/23 10:05 AM
 * profiling start/stop/status
 * start / stop / status
 * --d duration
 * --f filepath
 * @description:
 * @since 2025/8/23
 */
public class PFCommandExecutor implements WoodyCommandExecutor {

    private static final int DEFAULT_PROFILING_DURATION = 300;

    private volatile boolean profiling;
    private Thread profilingThread;
    private String file;
    private long startTime;

    @Override
    public String commandName() {
        return "pf";
    }

    @Override
    public boolean support(WoodyCommand command) {
        return command.getEval().startsWith(commandName() + " ");
    }

    @Override
    public void executeInternal(WoodyCommand command) {
        String[] segments = splitCommandEval(command);
        Operation operation = null;
        int duration = 0;
        int opCount = 0;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (segment.equals(commandName())) {
                continue;
            } else if (segment.equals("start")) {
                operation = Operation.START;
                opCount++;
            } else if (segment.equals("stop")) {
                operation = Operation.STOP;
                opCount++;
            } else if (segment.equals("status")) {
                operation = Operation.STATUS;
                opCount++;
            } else if (segment.equals("--d")) {
                if (operation != Operation.START) {
                    command.error("invalid command segment: " + segment);
                    return;
                }
                if (i == segments.length - 1) {
                    command.error("miss profiling duration param value!");
                    return;
                }
                duration = Integer.parseInt(segments[++i]);
            } else if (segment.equals("--f")) {
                if (i == segments.length - 1) {
                    command.error("miss profiling file param value!");
                    return;
                }
                file = segments[++i].trim();
                if (!file.endsWith(".html")) {
                    file = file + ".html";
                }
            } else {
                command.error("invalid profiling command!");
                return;
            }
        }
        if (opCount != 1) {
            command.error("invalid profiling command!");
            return;
        }
        if (operation == Operation.START) {
            if (profiling) {
                command.error("profiling already started!");
                return;
            }
            if (duration == 0) {
                duration = DEFAULT_PROFILING_DURATION;
            }
            Map<String, Long> intervals = ProfilingManager.INSTANCE.getEventIntervals();
            if (intervals.isEmpty()) {
                command.error("no event interval set!");
                return;
            }
            Map<String, Set<ResourceMethod>> resources = ResourceFetcherManager.INSTANCE.listAllSelectedResources();
            if (resources.isEmpty()) {
                command.error("no resource selected!");
                return;
            }
            startProfiling(command, duration);
        } else if (operation == Operation.STOP) {
            if (!profiling) {
                command.error("profiling not started or already stop!");
                return;
            }
            profilingThread.interrupt();
        } else {
            if (profiling) {
                long timeDelta = System.currentTimeMillis() - startTime;
                command.result(String.format("has profiling %d seconds!", timeDelta / 1000));
            } else {
                command.result("off profiling");
            }
        }
    }

    private void startProfiling(WoodyCommand command, int duration) {
        try {
            ProfilingManager.INSTANCE.startProfiling();
            command.result("start profiling successful!");
            startTime = System.currentTimeMillis();

            profilingThread = AgentThreadFactory.newAgentThread(AgentThreadFactory.AgentThread.PROFILING_WORKER, () -> {
                try {
                    profiling = true;
                    Thread.sleep(duration * 1000);
                } catch (InterruptedException e1) {
                } finally {
                    profiling = false;
                    profilingThread = null;
                    stopProfiling(command);
                }
            });
            profilingThread.start();
        } catch (Throwable e) {
            command.error(buildFailedMessage(command.getEval(), e));
        }
    }

    private void stopProfiling(WoodyCommand command) {
        try {
            List<ProfilingSample> profilingSamples = ProfilingManager.INSTANCE.stopProfiling();
            ProfilingSampleProcessor.populateTracingInfo(profilingSamples, TraceManager.PROFILING_TRACES);
            Map<String, ProfilingSampleBase> sampleBaseMap = ProfilingSampleProcessor.extractSampleBase(profilingSamples);
            Map<String, List<ProfilingSample>> eventSamples = profilingSamples.stream().collect(Collectors.groupingBy(ProfilingSample::getEventType));

            // 命令指定生成文件
            if (file != null) {
                boolean singleEvent = eventSamples.size() == 1;
                String parent = WoodyBootstrap.getInstance().getWoodyHome() + File.separator;
                for (Map.Entry<String, List<ProfilingSample>> entry : eventSamples.entrySet()) {
                    String output = parent + (singleEvent ? "" : entry.getKey()) + file;
                    File file = new File(output);
                    if (file.exists()) {
                        file.delete();
                    }
                    FlameGraph.convert(entry.getValue(), sampleBaseMap, output);
                }
            } else {
                // 缓存trace和样本
                ProfilingManager.INSTANCE.setTraceSamples(new TraceSamples(sampleBaseMap, eventSamples));
            }
            command.result("profiling finished!");
        } catch (Throwable t) {
            command.error(buildFailedMessage(command.getEval(), t));
        } finally {
            file = null;
            command.setTime(System.currentTimeMillis());
            command.setAsync(false);
            WoodyServerHandler.write(command);
        }
    }

    private enum Operation {
        START, STOP, STATUS
    }
}
