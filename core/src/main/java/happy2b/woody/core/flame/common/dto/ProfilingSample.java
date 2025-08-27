package happy2b.woody.core.flame.common.dto;

import java.util.ArrayList;
import java.util.List;

public class ProfilingSample {
    private Long sampleTime;
    private String onOperation;
    private transient Long spanId;
    private Object traceId;
    private int ticks;
    private String resource;
    private Integer rsFlagIndex;
    private String rsType;
    private transient Long tid;
    private String eventType;
    private String frameTypeIds;
    private Integer instanceAlloc;
    private List<String> stackTraces = new ArrayList<>(128);

    public void adStackTrace(String stackTrace) {
        stackTraces.add(stackTrace);
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setSampleTime(Long sampleTime) {
        this.sampleTime = sampleTime;
    }

    public Object getTraceId() {
        return traceId;
    }

    public void setTraceId(Object traceId) {
        this.traceId = traceId;
    }

    public Integer getRsFlagIndex() {
        return rsFlagIndex;
    }

    public void setRsFlagIndex(Integer rsFlagIndex) {
        this.rsFlagIndex = rsFlagIndex;
    }

    public List<String> getStackTraces() {
        return stackTraces;
    }

    public void setStackTraces(List<String> stackTraces) {
        this.stackTraces = stackTraces;
    }

    public String getRsType() {
        return rsType;
    }

    public void setRsType(String rsType) {
        this.rsType = rsType;
    }

    public void setTid(Long tid) {
        this.tid = tid;
    }

    public Long getTid() {
        return tid;
    }

    public long getSampleTime() {
        return sampleTime;
    }

    public void setSampleTime(long sampleTime) {
        this.sampleTime = sampleTime;
    }

    public String getFrameTypeIds() {
        return frameTypeIds;
    }

    public void setFrameTypeIds(String frameTypeIds) {
        this.frameTypeIds = frameTypeIds;
    }

    public void setInstanceAlloc(Integer instanceAlloc) {
        this.instanceAlloc = instanceAlloc;
    }

    public Integer getInstanceAlloc() {
        return instanceAlloc;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getOperation() {
        return onOperation;
    }

    public void setOnOperation(String onOperation) {
        this.onOperation = onOperation;
    }

    public Long getSpanId() {
        return spanId;
    }

    public void setSpanId(Long spanId) {
        this.spanId = spanId;
    }

    public int getTicks() {
        return ticks;
    }

    public void setTicks(int ticks) {
        this.ticks = ticks;
    }
}
