package happy2b.woody.common.constant;

import java.util.HashMap;
import java.util.Map;

public enum ProfilingEvent {
    CPU("cpu"),
    ALLOC("alloc"),
    WALL("wall"),
    LOCK("lock");

    private static final Map<String, ProfilingEvent> VALUES = new HashMap<>();

    static {
        for (ProfilingEvent value : values()) {
            VALUES.put(value.segment, value);
        }
    }

    private String segment;

    ProfilingEvent(String segment) {
        this.segment = segment;
    }

    public String getSegment() {
        return segment;
    }

    public static ProfilingEvent ofValue(String segment) {
        return VALUES.get(segment);
    }
}
