package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/21
 */
public abstract class CommandExecutors {

    private static final List<WoodyCommandExecutor> WOODY_COMMANDS = new ArrayList<>();

    static {
        WOODY_COMMANDS.add(new StopCommandExecutor());
        WOODY_COMMANDS.add(new PRCommandExecutor());
        WOODY_COMMANDS.add(new PECommandExecutor());
        WOODY_COMMANDS.add(new PFCommandExecutor());
        WOODY_COMMANDS.add(new TSCommandExecutor());
        WOODY_COMMANDS.add(new FNCommandExecutor());
        WOODY_COMMANDS.add(new IGCommandExecutor());
    }

    public static WoodyCommand execute(String cmdEval) {
        WoodyCommand command = new WoodyCommand(System.currentTimeMillis(), cmdEval);
        for (WoodyCommandExecutor executor : WOODY_COMMANDS) {
            if (executor.support(command)) {
                command.setCommandName(executor.commandName());
                executor.execute(command);
                return command;
            }
        }
        command.error("Unsupport command:" + cmdEval);
        return command;
    }

    public static void destroy() {
        WOODY_COMMANDS.clear();
    }

}
