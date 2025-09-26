package happy2b.woody.core.manager;

import happy2b.woody.common.func.WoodyFunction;
import happy2b.woody.common.id.CustomizeIdGenerator;
import happy2b.woody.common.id.IdGenerator;
import happy2b.woody.common.id.ThreadLocalRandomIdGenerator;
import happy2b.woody.common.id.TimeBasedIdGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/9/21
 */
public class IdGeneratorManager {

    public static IdGeneratorManager INSTANCE = new IdGeneratorManager();

    public IdGeneratorManager() {
        IdGenerator.ID_GENERATORS[0] = TimeBasedIdGenerator.INSTANCE;
        IdGenerator.ID_GENERATORS[1] = ThreadLocalRandomIdGenerator.INSTANCE;
    }

    public int createIdGenerator(int paramIndex, WoodyFunction function) {
        CustomizeIdGenerator generator = new CustomizeIdGenerator(paramIndex, function);
        IdGenerator.ID_GENERATORS[generator.getOrder()] = generator;
        return generator.getOrder();
    }

    public CustomizeIdGenerator findIdGenerator(int index) {
        return (CustomizeIdGenerator) IdGenerator.ID_GENERATORS[index];
    }

    public void clearIdGenerators() {
        Arrays.fill(IdGenerator.ID_GENERATORS, null);
    }

    public List<CustomizeIdGenerator> listAllIdGenerators() {
        List<CustomizeIdGenerator> list = new ArrayList<>();
        for (IdGenerator idGenerator : IdGenerator.ID_GENERATORS) {
            if (idGenerator instanceof CustomizeIdGenerator) {
                list.add((CustomizeIdGenerator) idGenerator);
            }
        }
        return list;
    }

    public static void destroy() {
        INSTANCE.clearIdGenerators();
        INSTANCE = null;
    }

}
