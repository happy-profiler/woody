package happy2b.woody.core.manager;

import happy2b.woody.common.id.IdGenerator;
import happy2b.woody.common.api.ResourceMethod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/7/30
 */
public class ResourceMethodManager {

    public static ResourceMethodManager INSTANCE = new ResourceMethodManager();

    public List<ResourceMethod> allProfilingIncludeMethods = new ArrayList<>();
    public Map<String,ResourceMethod> methodPathResourceMappings = new ConcurrentHashMap<>();
    public Set<String> selectedResourceTypes = ConcurrentHashMap.newKeySet();
    public Set<ResourceMethod> selectedProfilingIncludeMethods = ConcurrentHashMap.newKeySet();

    public Set<String> tracingMethods = ConcurrentHashMap.newKeySet();

    private ResourceMethodManager() {
    }

    public void addProfilingIncludeMethod(ResourceMethod method) {
        allProfilingIncludeMethods.add(method);
        tracingMethods.add(method.getClazz().getName().replace(".", "/"));
        methodPathResourceMappings.put(method.getMethodPath(), method);

        int order = method.getIdGenerator().getOrder();
        IdGenerator.ID_GENERATORS[order] = method.getIdGenerator();
    }

    public ResourceMethod findProfilingIncludeMethod(String className, String methodName, String descriptor) {
        for (ResourceMethod method : selectedProfilingIncludeMethods) {
            if (method.getClazz().getName().equals(className) && method.getMethodName().equals(methodName) && method.getSignature().equals(descriptor)) {
                return method;
            }
        }
        return null;
    }

    public Map<String, String> buildResourceTypeMappings() {
        Map<String, String> mappings = new HashMap<>();
        for (ResourceMethod method : allProfilingIncludeMethods) {
            mappings.put(method.getResource(), method.getResourceType());
        }
        return mappings;
    }

    public Map<String, String> buildMethodPathResourceMappings() {
        Map<String, String> mappings = new HashMap<>();
        for (ResourceMethod method : allProfilingIncludeMethods) {
            mappings.put(method.getMethodPath(), method.getResource());
        }
        return mappings;
    }

    public Set<ResourceMethod> getResourceByType(String resourceType) {
        Set<ResourceMethod> methods = new HashSet<>();
        for (ResourceMethod includeMethod : allProfilingIncludeMethods) {
            if (includeMethod.getResourceType().equals(resourceType)) {
                methods.add(includeMethod);
            }
        }
        return methods;
    }

    public Set<ResourceMethod> getSelectedResourceByType(String resourceType) {
        Set<ResourceMethod> methods = new HashSet<>();
        for (ResourceMethod includeMethod : selectedProfilingIncludeMethods) {
            if (includeMethod.getResourceType().equals(resourceType)) {
                methods.add(includeMethod);
            }
        }
        return methods;
    }

    public void addSelectedResourceMethod(ResourceMethod resourceMethod) {
        selectedProfilingIncludeMethods.add(resourceMethod);
        selectedResourceTypes.add(resourceMethod.getResourceType());
    }

    public void addSelectedResourceMethod(Collection<ResourceMethod> resourceMethods) {
        selectedProfilingIncludeMethods.addAll(resourceMethods);
        for (ResourceMethod resourceMethod : resourceMethods) {
            selectedResourceTypes.add(resourceMethod.getResourceType());
        }
    }

    public void deleteSelectedResources(String type, Integer... orders) {
        if (orders == null || orders.length == 0) {
            selectedResourceTypes.remove(type);
            selectedProfilingIncludeMethods.removeIf(method -> method.getResourceType().equals(type));
            return;
        }

        for (Integer order : orders) {
            selectedProfilingIncludeMethods.removeIf(method -> method.getResourceType().equals(type) && method.getOrder() == order);
        }
        selectedResourceTypes = selectedProfilingIncludeMethods.stream().map(ResourceMethod::getResourceType).collect(Collectors.toSet());
    }

    public ResourceMethod findResourceMethodByMethodPath(String methodPath) {
        return methodPathResourceMappings.get(methodPath);
    }

    public Set<String> getSelectedResourceTypes() {
        return selectedResourceTypes;
    }

    public static void destroy() {
        if (INSTANCE != null) {
            INSTANCE.allProfilingIncludeMethods = null;
            INSTANCE.selectedProfilingIncludeMethods = null;
            INSTANCE.tracingMethods = null;
            INSTANCE = null;
        }
    }

}
