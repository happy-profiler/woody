package happy2b.woody.common.api;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/22
 */
public class WoodyCommand {
    private String commandName;
    private long time;
    private String eval;
    private String result;
    private boolean success;
    private boolean async = false;
    private long executeTime;

    public WoodyCommand(long time, String eval) {
        this.time = time;
        this.eval = eval;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getEval() {
        return eval;
    }

    public void setEval(String eval) {
        this.eval = eval;
    }

    public String getResult() {
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public void result(String result) {
        this.result = result;
        this.success = true;
        this.executeTime = System.currentTimeMillis();
    }

    public void error(String error) {
        this.result = error;
        this.success = false;
        this.executeTime = System.currentTimeMillis();
    }
}
