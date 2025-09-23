package happy2b.woody.core.manager;

import happy2b.woody.core.common.func.WoodyFunction;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/9/16
 */
public class FunctionManager {

    public static FunctionManager INSTANCE = new FunctionManager();

    private AtomicInteger sequence = new AtomicInteger(1);
    private Map<Integer, WoodyFunction> functions = new ConcurrentHashMap<>();

    private FunctionManager() {
    }

    public int newFunction(boolean isFilter, String expression) {
        int i = sequence.getAndIncrement();
        WoodyFunction function = new WoodyFunction(i, isFilter, expression);
        functions.put(i, function);
        return i;
    }

    public List<WoodyFunction> listAllFunctions() {
        return functions.values().stream().sorted(Comparator.comparingInt(WoodyFunction::getOrder)).collect(Collectors.toList());
    }

    public void clearFunctions() {
        functions.clear();
    }

    public WoodyFunction findFunction(int order) {
        return functions.get(order);
    }

    public static void destroy() {
        INSTANCE.functions.clear();
        INSTANCE = null;
    }

}
