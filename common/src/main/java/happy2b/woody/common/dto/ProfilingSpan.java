package happy2b.woody.common.dto;

import happy2b.woody.common.api.NanoTimer;

import java.woody.SpyAPI;

public class ProfilingSpan implements SpyAPI.ISpan {
  private Object traceId;
  private Object spanId;
  private long startTime;
  private String operation;
  private String rootMethod;
  private long endTime;

  public ProfilingSpan(Object traceId, Object spanId, long startTime, String operationName, String resource) {
    this.traceId = traceId;
    this.spanId = spanId;
    this.startTime = startTime;
    this.operation = operationName;
    this.rootMethod = resource;
  }

  public Object getSpanId() {
    return spanId;
  }

  public void setSpanId(long spanId) {
    this.spanId = spanId;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public Object getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public boolean contains(long timestamp) {
    return timestamp >= startTime && timestamp <= endTime;
  }

  public String getRootMethod() {
    return rootMethod;
  }

  @Override
  public void finish() {
    this.endTime = NanoTimer.INSTANCE.getNanoTime();
  }
}
