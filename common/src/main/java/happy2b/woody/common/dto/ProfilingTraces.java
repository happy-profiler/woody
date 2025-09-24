package happy2b.woody.common.dto;

import java.util.ArrayList;
import java.util.List;

public class ProfilingTraces {

  private List<ProfilingTrace> traceList = new ArrayList<>(64);

  public int getTraceNum() {
    return traceList.size();
  }

  public void startProfilingTrace(ProfilingTrace trace) {
    this.traceList.add(trace);
  }

  public ProfilingSpan startProfilingSpan(Object spanId, long time, String operationName) {
    if (traceList.isEmpty()) {
      return null;
    }
    return traceList.get(traceList.size() - 1).startSpan(spanId, time, operationName);
  }

  public List<ProfilingTrace> getTraceList() {
    return traceList;
  }


  public ProfilingTrace findTraceByTime(long sampleTime, String resource) {
    if (traceList.isEmpty()) {
      return null;
    }
    long startTime = traceList.get(0).getStartTime();
    long endTime = traceList.get(traceList.size() - 1).getEndTime();
    if (sampleTime < startTime || sampleTime > endTime) {
      return null;
    }

    int left = 0;
    int right = traceList.size() - 1;

    ProfilingTrace trace = null;
    while (left <= right) {
      int mid = left + (right - left) / 2;
      trace = traceList.get(mid);
      if (trace.contains(sampleTime)) {
        trace = traceList.get(mid);
        break;
      } else if (sampleTime < trace.getStartTime()) {
        right = mid - 1;
      } else {
        left = mid + 1;
      }
    }

    if (trace != null && !trace.getResource().equals(resource)) {
      trace = null;
    }

    if (trace == null) {
      int x = left < right ? left : right;
      for (int i = x; i < traceList.size(); i++) {
        ProfilingTrace t = traceList.get(i);
        if ((t.getStartTime() - 5_000_000) < sampleTime && t.getResource().equals(resource)) {
          trace = t;
          break;
        }
        if (t.getStartTime() > (sampleTime + 10_000_000)) {
          break;
        }
      }
    }
    if (trace == null || !trace.getResource().equals(resource)) {
      return null;
    }
    return trace;
  }

}
