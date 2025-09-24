package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.common.api.WoodyCommandExecutor;
import happy2b.woody.common.id.IdGenerator;
import happy2b.woody.common.constant.ProfilingResourceType;
import happy2b.woody.common.id.ThreadLocalRandomIdGenerator;
import happy2b.woody.core.manager.ResourceFetcherManager;
import happy2b.woody.common.api.ResourceMethod;
import happy2b.woody.core.manager.IdGeneratorManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jiangjibo
 * @version 1.0
 * @description: profiling resource
 * -ls(list resource) / -lt(list resource type) / -lss(list select resource) / -lst(list select type)
 * -us(unselect) / -s(select)
 * --type kafka
 * --order 1,2,3
 * --id(id generator index) 1
 * @since 2025/8/25
 */
public class PRCommandExecutor implements WoodyCommandExecutor {

    PRCommandExecutor() {
    }

    @Override
    public String commandName() {
        return "pr";
    }

    @Override
    public boolean support(WoodyCommand command) {
        return command.getEval().startsWith(commandName() + " ");
    }

    @Override
    public void executeInternal(WoodyCommand command) {
        int opCount = 0;
        IdGenerator idGenerator = ThreadLocalRandomIdGenerator.INSTANCE;
        String type = null;
        String orderSegment = null;
        boolean select = false, unselect = false;
        boolean listResources = false, listSelectedResources = false, listSelectedResourceTypes = false, listType = false;

        String[] segments = splitCommandEval(command);
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (segment.equals(commandName())) {

            } else if (segment.equals("-ls")) {
                listResources = true;
                opCount++;
            } else if (segment.equals("-lt")) {
                listType = true;
                opCount++;
            } else if (segment.equals("-lss")) {
                listSelectedResources = true;
                opCount++;
            } else if (segment.equals("-lst")) {
                listSelectedResourceTypes = true;
                opCount++;
            } else if (segment.equals("-s")) {
                select = true;
                opCount++;
            } else if (segment.equals("-us")) {
                unselect = true;
                opCount++;
            } else if (segment.equals("--type")) {
                if (i == segments.length - 1) {
                    command.error("miss profiling resource type param value!");
                    return;
                }
                type = segments[++i].trim();
                if (ProfilingResourceType.ofType(type) == null) {
                    command.error("invalid resource type: " + type);
                    return;
                }
            } else if (segment.equals("--order")) {
                if (ProfilingResourceType.ofType(type) == null) {
                    command.error("miss profiling resource order param value!");
                    return;
                }
                orderSegment = segments[++i].trim();
            } else if (segment.equals("--id")) {
                if (i == segments.length - 1) {
                    command.error("missing profiling resource 'id generator index'!");
                    return;
                }
                int idGeneratorIndex = Integer.parseInt(segments[++i].trim());
                idGenerator = IdGeneratorManager.INSTANCE.findIdGenerator(idGeneratorIndex);
                if (idGenerator == null) {
                    command.error("invalid profiling resource id generator index :" + idGenerator + " !");
                    return;
                }
            } else {
                command.error("profiling event type not support other arguments!");
                return;
            }
        }

        if (opCount != 1) {
            command.error("only support one operation!");
            return;
        }

        if (listType) {
            listProfilingResourceTypes(command);
            return;
        }

        if (listSelectedResources) {
            listSelectedResources(command, type);
            return;
        }

        if (listSelectedResourceTypes) {
            listSelectedResourceTypes(command);
            return;
        }

        if (listResources) {
            listProfilingResources(command, select, type);
            return;
        }

        if (select) {
            selectResources(command, type, orderSegment, idGenerator);
            return;
        }

        if (unselect) {
            unselectResources(command, type, orderSegment);
            return;
        }

    }

    private void listProfilingResourceTypes(WoodyCommand command) {
        List<String> types = ResourceFetcherManager.INSTANCE.listAllAvailableResourceTypes();
        String collect = types.stream().collect(Collectors.joining(","));
        command.result("[" + collect + "]");
    }

    private void listProfilingResources(WoodyCommand command, boolean selected, String type) {
        StringBuilder sb = new StringBuilder();
        if (selected) {
            if (type != null) {
                Set<ResourceMethod> methods = ResourceFetcherManager.INSTANCE.listSelectedResources(type);
                appendResourceFormatString(sb, type, methods);
            } else {
                Map<String, Set<ResourceMethod>> methods = ResourceFetcherManager.INSTANCE.listAllSelectedResources();
                for (Map.Entry<String, Set<ResourceMethod>> entry : methods.entrySet()) {
                    appendResourceFormatString(sb, entry.getKey(), entry.getValue());
                }
            }
        } else {
            if (type != null) {
                Set<ResourceMethod> methods = ResourceFetcherManager.INSTANCE.listResources(type);
                appendResourceFormatString(sb, type, methods);
            } else {
                Map<String, Set<ResourceMethod>> methods = ResourceFetcherManager.INSTANCE.listAllResources();
                for (Map.Entry<String, Set<ResourceMethod>> entry : methods.entrySet()) {
                    appendResourceFormatString(sb, entry.getKey(), entry.getValue());
                }
            }
        }
        command.result(sb.toString().trim());
    }

    private void listSelectedResources(WoodyCommand command, String type) {
        StringBuilder sb = new StringBuilder();
        if (type == null) {
            Map<String, Set<ResourceMethod>> methods = ResourceFetcherManager.INSTANCE.listAllSelectedResources();
            for (Map.Entry<String, Set<ResourceMethod>> entry : methods.entrySet()) {
                appendResourceFormatString(sb, entry.getKey(), entry.getValue());
            }
        } else {
            Set<ResourceMethod> methods = ResourceFetcherManager.INSTANCE.listSelectedResources(type);
            appendResourceFormatString(sb, type, methods);
        }
        command.result("[" + sb.toString().trim() + "]");
    }

    private void listSelectedResourceTypes(WoodyCommand command) {
        command.result("[" + ResourceFetcherManager.INSTANCE.listSelectedResourceTypes().stream().collect(Collectors.joining(",")) + "]");
    }

    private void selectResources(WoodyCommand command, String type, String orderSegment, IdGenerator idGenerator) {
        if (orderSegment == null) {
            ResourceFetcherManager.INSTANCE.selectResources(idGenerator, type);
        } else {
            List<Integer> orders = null;
            if (orderSegment != null) {
                orders = new ArrayList<>();
                Set<ResourceMethod> methods = ResourceFetcherManager.INSTANCE.listResources(type);
                Set<Integer> allOrders = methods.stream().map(ResourceMethod::getOrder).collect(Collectors.toSet());
                for (String order : orderSegment.split(",")) {
                    int i = Integer.parseInt(order);
                    if (!allOrders.contains(i)) {
                        command.error("invalid order:" + order);
                        return;
                    }
                    orders.add(i);
                }
            }
            ResourceFetcherManager.INSTANCE.selectResources(idGenerator, type, orders);
        }
        command.result("select profiling resource success!");
    }

    private void unselectResources(WoodyCommand command, String type, String orderSegment) {
        List<Integer> orders = new ArrayList<>();
        if (orderSegment != null) {
            for (String order : orderSegment.split(",")) {
                orders.add(Integer.parseInt(order.trim()));
            }
        }
        ResourceFetcherManager.INSTANCE.deleteSelectedResources(type, orders.toArray(new Integer[0]));
        command.result("unselect profiling resource success!");
    }

    private void appendResourceFormatString(StringBuilder sb, String type, Set<ResourceMethod> methods) {
        if (sb.length() == 0) {
            sb.append("\n");
        }
        List<ResourceMethod> sortedMethods = methods.stream().sorted(Comparator.comparingInt(ResourceMethod::getOrder)).collect(Collectors.toList());
        sb.append(type).append(":\n");
        for (ResourceMethod method : sortedMethods) {
            String order = String.format("%3d", method.getOrder());
            sb.append("  ").append(order).append(". ").append(method.getResource()).append(", id generator: ").append(method.getIdGenerator().getOrder()).append("\n");
        }
    }
}
