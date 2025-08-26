package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.common.utils.AnsiLog;

public interface WoodyCommandExecutor {

    String commandName();

    boolean support(WoodyCommand command);

    void executeInternal(WoodyCommand command);

    default void execute(WoodyCommand command){
        try {
            executeInternal(command);
        } catch (Throwable throwable) {
            command.error(buildFailedMessage(command.getEval(), throwable));
        }
    }

    default String buildFailedMessage(String command, Throwable t) {
        AnsiLog.error(t);
        return "执行命令:'" + command + "’失败,失败信息:" + t.getMessage() + ", 相信错误 信息请查看${user.home}/.woody/woody.log文件";
    }

}
