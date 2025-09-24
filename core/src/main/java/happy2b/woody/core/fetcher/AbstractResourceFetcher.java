package happy2b.woody.core.fetcher;

import happy2b.woody.common.api.ResourceFetcher;
import happy2b.woody.common.api.ResourceMethod;
import happy2b.woody.core.manager.ResourceClassManager;
import happy2b.woody.core.manager.ResourceMethodManager;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/9/24
 */
public abstract class AbstractResourceFetcher implements ResourceFetcher {


    public boolean isSupport(Class clazz) {
        return Arrays.stream(getResourceClassName()).collect(Collectors.toSet()).contains(clazz.getName());
    }

    protected void addResourceMethod(ResourceMethod method) {
        Class<?> clazz = method.getMethod().getDeclaringClass();
        if (clazz.getName().startsWith("org.springframework")) {
            return;
        }
        ResourceMethodManager.INSTANCE.addProfilingIncludeMethod(method);
        String resourceType = method.getResourceType();
        AtomicInteger atomicInteger = resourceCounts.get(resourceType);
        if (atomicInteger == null) {
            atomicInteger = new AtomicInteger(0);
            resourceCounts.put(resourceType, atomicInteger);
        }
        method.setOrder(atomicInteger.incrementAndGet());
        ResourceClassManager.INSTANCE.addResourceClass(clazz);
    }


    protected Method findMostApplicableMethod(Class type, String method) {
        List<Method> namedMethods = new ArrayList<>();
        for (Method declaredMethod : type.getDeclaredMethods()) {
            if (!declaredMethod.getName().equals(method)) {
                continue;
            }
            namedMethods.add(declaredMethod);
        }
        if (namedMethods.isEmpty()) {
            return null;
        }
        if (namedMethods.size() == 1) {
            return namedMethods.get(0);
        }

        namedMethods = namedMethods.stream().filter(new Predicate<Method>() {
            @Override
            public boolean test(Method method) {
                return !(method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Object.class);
            }
        }).collect(Collectors.toList());

        return namedMethods.stream().sorted(new Comparator<Method>() {
            @Override
            public int compare(Method o1, Method o2) {
                // 优先取public方法
                boolean pub1 = Modifier.isPublic(o1.getModifiers());
                boolean pub2 = Modifier.isPublic(o2.getModifiers());
                if (pub1 && !pub2) {
                    return -1;
                }
                if (!pub1 && pub2) {
                    return 1;
                }
                // 优先取参数长的方法
                int l1 = o1.getParameterCount();
                int l2 = o2.getParameterCount();
                return l1 > l2 ? -1 : 1;
            }
        }).findFirst().get();

    }
}
