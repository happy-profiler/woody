package happy2b.woody.inst;



import happy2b.woody.common.api.InstrumentationProfiler;
import happy2b.woody.common.utils.InstrumentationUtils;
import happy2b.woody.inst.core.InstrumentationProfilingProcessor;

import java.lang.instrument.Instrumentation;

/**
 * 探针启动器
 *
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/10
 */
public class InstrumentationProfilerBootstrap {
    public static void premain(String agentArgs, Instrumentation inst) {
        InstrumentationUtils.setInstrumentation(inst);
        InstrumentationProfiler.INSTANCE_REFERENCE.set(new InstrumentationProfilingProcessor());
    }
}