package happy2b.woody.common.id;

import happy2b.woody.common.api.NanoTimer;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/9/24
 */
public class TimeBasedIdGenerator implements IdGenerator<Long> {

    public static final TimeBasedIdGenerator INSTANCE = new TimeBasedIdGenerator();

    private int order;

    private TimeBasedIdGenerator() {
        this.order = ORDER.incrementAndGet();
    }

    @Override
    public Long generateTraceId() {
        return NanoTimer.INSTANCE.getNanoTime() / 1000;
    }

    @Override
    public Long generateSpanId() {
        return NanoTimer.INSTANCE.getNanoTime() / 1000;
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
