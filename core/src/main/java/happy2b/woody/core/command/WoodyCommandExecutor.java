package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.common.utils.AnsiLog;

import java.util.Arrays;
import java.util.stream.Collectors;

public interface WoodyCommandExecutor {

    String commandName();

    boolean support(WoodyCommand command);

    void executeInternal(WoodyCommand command) throws Throwable;

    default void execute(WoodyCommand command) {
        try {
            executeInternal(command);
        } catch (Throwable throwable) {
            command.error(buildFailedMessage(command.getEval(), throwable));
        }
    }

    default String[] splitCommandEval(WoodyCommand command) {
        String[] segments = command.getEval().split(" ");
        return Arrays.stream(segments).filter(s -> !s.isEmpty()).collect(Collectors.toList()).toArray(new String[0]);
    }

    default String buildFailedMessage(String command, Throwable t) {
        AnsiLog.error(t);
        return "执行命令:'" + command + "’失败,失败信息:" + t.getMessage() + ", 相信错误 信息请查看${user.home}/.woody/woody.log文件";
    }

}
