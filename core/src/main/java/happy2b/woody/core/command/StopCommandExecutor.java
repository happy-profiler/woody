package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.common.api.WoodyCommandExecutor;
import happy2b.woody.core.server.WoodyBootstrap;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/21
 */
public class StopCommandExecutor implements WoodyCommandExecutor {

    public static final String COMMAND_NAME = "stop";

    StopCommandExecutor() {
    }

    @Override
    public String commandName() {
        return COMMAND_NAME;
    }

    @Override
    public boolean support(WoodyCommand command) {
        return COMMAND_NAME.equalsIgnoreCase(command.getEval());
    }

    @Override
    public void executeInternal(WoodyCommand command) {
        WoodyBootstrap instance = WoodyBootstrap.getInstance();
        if (instance != null) {
            instance.destroy();
        }
    }
}
