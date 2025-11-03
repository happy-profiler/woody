package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.common.api.WoodyCommandExecutor;
import happy2b.woody.common.id.CustomizeIdGenerator;
import happy2b.woody.common.func.WoodyFunction;
import happy2b.woody.common.id.OpenTelemetryIdExtractor;
import happy2b.woody.common.id.ThreadLocalRandomIdGenerator;
import happy2b.woody.common.id.TimeBasedIdGenerator;
import happy2b.woody.core.manager.FunctionManager;
import happy2b.woody.core.manager.IdGeneratorManager;

import java.util.Comparator;
import java.util.List;

/**
 * ID Generator
 * ig [operator] [arguments]
 * -n : new
 * -c : clear
 * -l : list
 * --target : this/param[index]
 * --fn : function order
 *
 * @author jiangjibo
 * @version 1.0
 * @since 2025/9/21
 */
public class IGCommandExecutor implements WoodyCommandExecutor {

    @Override
    public String commandName() {
        return "ig";
    }

    @Override
    public boolean support(WoodyCommand command) {
        return command.getEval().startsWith(commandName());
    }

    @Override
    public void executeInternal(WoodyCommand command) {
        boolean create = false, clear = false, list = false;
        String target = null;
        int funcOrder = -1;
        int opCount = 0;
        String[] segments = splitCommandEval(command);
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (segment.equals(commandName())) {
                continue;
            }
            if (segment.equals("-n")) {
                create = true;
                opCount++;
                continue;
            }
            if (segment.equals("-l")) {
                list = true;
                opCount++;
                continue;
            }
            if (segment.equals("-c")) {
                clear = true;
                opCount++;
                continue;
            }
            if (segment.equals("--target")) {
                if (i == segments.length - 1) {
                    command.error("no target specified!");
                    return;
                }
                target = segments[++i];
                if (!target.equals("this") && !target.startsWith("param[")) {
                    command.error("invalid target: " + target);
                    return;
                }
            }
            if (segment.equals("--fn")) {
                if (i == segments.length - 1) {
                    command.error("no function order specified!");
                    return;
                }
                funcOrder = Integer.parseInt(segments[++i]);
                WoodyFunction function = FunctionManager.INSTANCE.findFunction(funcOrder);
                if (function == null) {
                    command.error("function " + funcOrder + " not found!");
                    return;
                }
            }
        }

        if (opCount > 1) {
            command.error("too many operations");
            return;
        }

        if (create) {
            if (funcOrder == -1) {
                command.error("no function order specified!");
                return;
            }
            if (target.startsWith("param[")) {
                int paramIndex = Integer.parseInt(target.substring(6, target.length() - 1));
                int order = IdGeneratorManager.INSTANCE.createIdGenerator(paramIndex, FunctionManager.INSTANCE.findFunction(funcOrder));
                command.result("create id generator success, id generator order: " + order);
            } else {
                command.error("not support generating id from 'this'!");
            }
            return;
        }

        if (list) {
            List<CustomizeIdGenerator> generators = IdGeneratorManager.INSTANCE.listAllIdGenerators();
            command.result(formatIdGenerator(generators));
            return;
        }

        if (clear) {
            IdGeneratorManager.INSTANCE.clearIdGenerators();
            return;
        }

    }

    private String formatIdGenerator(List<CustomizeIdGenerator> generators) {

        generators.sort(Comparator.comparingInt(CustomizeIdGenerator::getOrder));

        StringBuilder sb = new StringBuilder("woody id generators:").append("\n");

        sb.append(TimeBasedIdGenerator.INSTANCE.getOrder()).append(": TimeBased TraceId Generator").append("\n");
        sb.append(ThreadLocalRandomIdGenerator.INSTANCE.getOrder()).append(": ThreadLocalRandom TraceId Generator").append("\n");
        sb.append(OpenTelemetryIdExtractor.INSTANCE.getOrder()).append(": OpenTelemetry TraceId Extractor");

        for (CustomizeIdGenerator generator : generators) {
            sb.append("\n");
            sb.append(generator.getOrder()).append(": fn-order:").append(generator.getOrder()).append(", fn-exp:").append(generator.getFunction().getExpression());
        }
        return sb.toString();
    }

}
