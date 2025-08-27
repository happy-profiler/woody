package happy2b.woody.core.flame.resource.fetch;

import happy2b.woody.core.flame.common.constant.ProfilingResourceType;
import happy2b.woody.core.flame.resource.ResourceMethod;
import happy2b.woody.core.flame.manager.ResourceMethodManager;
import happy2b.woody.core.flame.manager.ResourceClassManager;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface ResourceFetcher {

    Map<String, String> resources = new ConcurrentHashMap<>();
    Map<String, AtomicInteger> resourceCounts = new ConcurrentHashMap<>();

    String[] getResourceClassName();

    void fetchResources(Class clazz);

    ProfilingResourceType resourceType();

    default boolean isSupport(Class clazz) {
        return Arrays.stream(getResourceClassName()).collect(Collectors.toSet()).contains(clazz.getName());
    }

    default void addResourceMethod(ResourceMethod method) {
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


    default Method findMostApplicableMethod(Class type, String method) {
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
