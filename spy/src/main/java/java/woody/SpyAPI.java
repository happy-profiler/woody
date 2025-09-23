package java.woody;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/25
 */
public class SpyAPI {

    public static final AbstractSpy NOPSPY = new NO_SPY();
    private static volatile AbstractSpy spyInstance = NOPSPY;


    public static volatile boolean INITED;

    public static AbstractSpy getSpy() {
        return spyInstance;
    }

    public static void setSpy(AbstractSpy spy) {
        spyInstance = spy;
    }

    public static void setNopSpy() {
        setSpy(NOPSPY);
    }

    public static boolean isNopSpy() {
        return NOPSPY == spyInstance;
    }

    public static void init() {
        INITED = true;
    }

    public static boolean isInited() {
        return INITED;
    }

    public static void destroy() {
        setNopSpy();
        INITED = false;
    }


    public static ITrace startTrace(String resourceType, String resource, String methodPath, int generatorIndex) {
        return spyInstance.startTrace(resourceType, resource, methodPath, generatorIndex);
    }

    public static ITrace startTrace(String resourceType, String resource, String methodPath, int generatorIndex, Object param) {
        return spyInstance.startTrace(resourceType, resource, methodPath, generatorIndex, param);
    }

    public static ISpan startSpan(String operationName, String methodPath, int generatorIndex) {
        return spyInstance.startSpan(operationName, methodPath, generatorIndex);
    }

    public static ISpan startSpan(String operationName, String methodPath, int generatorIndex, Object param) {
        return spyInstance.startSpan(operationName, methodPath, generatorIndex, param);
    }


    public static abstract class AbstractSpy {
        public abstract ITrace startTrace(String resourceType, String resource, String methodPath, int generatorIndex);

        public abstract ITrace startTrace(String resourceType, String resource, String methodPath, int generatorIndex, Object target);

        public abstract ISpan startSpan(String operationName, String methodPath, int generatorIndex);

        public abstract ISpan startSpan(String operationName, String methodPath, int generatorIndex, Object target);
    }

    public interface ITrace {
        void finish();
    }

    public interface ISpan {
        void finish();
    }


    public static final ITrace NO_OP_TRACE = new ITrace() {
        @Override
        public void finish() {
        }
    };

    public static final ISpan NO_OP_SPAN = new ISpan() {
        @Override
        public void finish() {
        }
    };

    public static class NO_SPY extends AbstractSpy {

        @Override
        public ITrace startTrace(String resourceType, String resource, String methodPath, int generatorIndex) {
            return NO_OP_TRACE;
        }

        @Override
        public ITrace startTrace(String resourceType, String resource, String methodPath, int generatorIndex, Object param) {
            return NO_OP_TRACE;
        }

        @Override
        public ISpan startSpan(String operationName, String methodPath, int generatorIndex) {
            return NO_OP_SPAN;
        }

        @Override
        public ISpan startSpan(String operationName, String methodPath, int generatorIndex, Object param) {
            return NO_OP_SPAN;
        }
    }

}
