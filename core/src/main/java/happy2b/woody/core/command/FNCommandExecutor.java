package happy2b.woody.core.command;

import happy2b.woody.common.api.WoodyCommand;
import happy2b.woody.core.common.func.WoodyFunction;
import happy2b.woody.core.manager.FunctionManager;

import java.util.List;

/**
 * Function 自定义函数命令，用于定义表达式，过滤上下文或者生成id
 * fn [operation] [arguments]
 * -b: boolean值, 表达式执行生成boolean值, 未指定时表达式是取值，用于生成id
 * -n: new function
 * -l: 列举已创建的function列表
 * -c: 清空所有function
 * --exp: 表达式,以两个'##'开头, boolean表达式目前不支持 && || 等连接符
 * 提取id表达式格式: .field/method(param1,param2)
 *              ##[0].filed1.field2; "[x]"表示数组或者list的元素, x表示下标; "."表示对象的属性或者map的key; 可多层递进
 *              ##getHeader("serviceId")
 *              ##getService(null) //空参数
 * 过滤上下文表达式格式: ##filed1.field2 == 'aaaa'; :##[0].filed1.field2 == 101 ; :#target[0].filed1.field2 == #target[1].field3
 *
 * @author jiangjibo
 * @version 1.0
 * @since 2025/9/16
 */
public class FNCommandExecutor implements WoodyCommandExecutor {

    @Override
    public String commandName() {
        return "fn";
    }

    @Override
    public boolean support(WoodyCommand command) {
        return command.getEval().startsWith(commandName() + " ");
    }

    @Override
    public void executeInternal(WoodyCommand command) throws Throwable {
        String[] segments = splitCommandEval(command);
        String exp = null;
        int opCount = 0;
        boolean filter = false;
        boolean create = false, list = false, clear = false;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (segment.equals(commandName())) {
                continue;
            }
            if (segment.equals("-b")) {
                filter = true;
                continue;
            }
            if (segment.equals("--exp")) {
                if (i == segments.length - 1) {
                    command.error("missing function command expression param value!");
                    return;
                }
                exp = segments[++i];
                if (!exp.startsWith("##")) {
                    command.error("invalid function command expression: " + exp);
                    return;
                }
                continue;
            }
            if (segment.equals("-c")) {
                opCount++;
                clear = true;
                continue;
            }
            if (segment.equals("-l")) {
                opCount++;
                list = true;
                continue;
            }
            if (segment.equals("-n")) {
                opCount++;
                create = true;
                continue;
            }
        }

        if (opCount > 1) {
            command.error("too many operations!");
            return;
        }
        if (create && exp == null) {
            command.error("no expression!");
            return;
        }

        if (create) {
            int order = FunctionManager.INSTANCE.newFunction(filter, exp);
            command.result("create function success, function order: " + order);
            return;
        }

        if (list) {
            List<WoodyFunction> woodyFunctions = FunctionManager.INSTANCE.listAllFunctions();
            command.result(formatFunctions(woodyFunctions));
            return;
        }

        if (clear) {
            FunctionManager.INSTANCE.clearFunctions();
            command.result("clear functions success!");
            return;
        }

    }

    private String formatFunctions(List<WoodyFunction> functions) {
        StringBuilder sb = new StringBuilder("woody functions:").append("\n");
        for (WoodyFunction function : functions) {
            sb.append(function.getOrder()).append(": ").append(function.getExpression()).append("\n");
        }
        return sb.toString();
    }
}
