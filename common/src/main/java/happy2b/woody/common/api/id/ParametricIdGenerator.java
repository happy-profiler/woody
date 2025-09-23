package happy2b.woody.common.api.id;

import happy2b.woody.common.api.FunctionTokenExecutor;

public interface ParametricIdGenerator<T> extends IdGenerator {

    T generateTraceId(Object param, FunctionTokenExecutor[] executors);

    T generateSpanId(Object param, FunctionTokenExecutor[] executors);

    /**
     * 参数索引, 从0开始
     *
     * @return
     */
    int paramIndex();

    @Override
    default Object generateTraceId() {
        throw new UnsupportedOperationException("generateTraceId");
    }

    @Override
    default Object generateSpanId() {
        throw new UnsupportedOperationException("generateSpanId");
    }
}
