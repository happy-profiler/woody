package happy2b.woody.core.flame.manager;

import happy2b.woody.common.bytecode.InstrumentationUtils;
import happy2b.woody.common.utils.AnsiLog;
import happy2b.woody.core.flame.resource.transform.ResourceMethodTransformer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/30
 */
public class ResourceClassManager {

    public static ResourceClassManager INSTANCE = new ResourceClassManager();

    private Set<Class> allResourceClasses = ConcurrentHashMap.newKeySet();
    private Set<Class> transformedClass = ConcurrentHashMap.newKeySet();

    private AtomicBoolean transformerAdded = new AtomicBoolean(false);

    private ResourceMethodTransformer transformer = new ResourceMethodTransformer();

    private ResourceClassManager() {
    }

    public void addResourceClass(Class clazz) {
        allResourceClasses.add(clazz);
    }

    public void retransformResourceClasses(Set<Class> resourceClasses) {
        if (transformerAdded.compareAndSet(false, true)) {
            InstrumentationUtils.getInstrumentation().addTransformer(transformer, true);
        }
        for (Class clazz : resourceClasses) {
            if (transformedClass.contains(clazz)) {
                continue;
            }
            try {
                InstrumentationUtils.getInstrumentation().retransformClasses(clazz);
                transformedClass.add(clazz);
            } catch (Throwable e) {
                AnsiLog.error(e, "One-Profiler: Retransform class '{}' occur exception!", clazz.getName());
            }
        }
    }

    public static void destroy() {
        InstrumentationUtils.getInstrumentation().removeTransformer(INSTANCE.transformer);
        if (INSTANCE != null) {
            for (Class clazz : INSTANCE.transformedClass) {
                try {
                    InstrumentationUtils.getInstrumentation().retransformClasses(clazz);
                } catch (Exception e) {
                }
            }
            INSTANCE = null;
        }
    }
}
