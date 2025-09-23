package happy2b.woody.core.common;

import happy2b.woody.common.api.FunctionTokenExecutor;
import happy2b.woody.common.api.id.ParametricIdGenerator;
import happy2b.woody.core.common.func.WoodyFunction;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/9/21
 */
public class CustomizeIdGenerator implements ParametricIdGenerator<Object> {

    private int paramIndex;
    private int funcOrder;
    private WoodyFunction function;

    public CustomizeIdGenerator(int paramIndex, WoodyFunction function) {
        this.paramIndex = paramIndex;
        this.function = function;
        this.funcOrder = ORDER.incrementAndGet();
    }

    @Override
    public Object generateTraceId(Object param, FunctionTokenExecutor[] executors) {
        return function.apply(param, executors);
    }

    @Override
    public Object generateSpanId(Object param, FunctionTokenExecutor[] executors) {
        return function.apply(param, executors);
    }

    @Override
    public int paramIndex() {
        return paramIndex;
    }

    @Override
    public int getOrder() {
        return funcOrder;
    }

    public WoodyFunction getFunction() {
        return function;
    }
}
