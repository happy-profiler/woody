package happy2b.woody.common.api.id;

import java.util.concurrent.atomic.AtomicInteger;

public interface IdGenerator<T> {

    AtomicInteger ORDER = new AtomicInteger(-1);

    IdGenerator INSTANCE = ThreadLocalRandomIdGenerator.INSTANCE;

    IdGenerator[] ID_GENERATORS = new IdGenerator[64];

    T generateTraceId();

    T generateSpanId();

    int getOrder();

}
