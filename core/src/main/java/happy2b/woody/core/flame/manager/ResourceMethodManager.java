package happy2b.woody.core.flame.manager;

import happy2b.woody.common.api.id.IdGenerator;
import happy2b.woody.core.flame.resource.ResourceMethod;

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

    private Set<Integer> generatorIndexes = ConcurrentHashMap.newKeySet();

    public List<ResourceMethod> allProfilingIncludeMethods = new ArrayList<>();
    public Set<String> selectedResourceTypes = ConcurrentHashMap.newKeySet();
    public Set<ResourceMethod> selectedProfilingIncludeMethods = ConcurrentHashMap.newKeySet();

    public IdGenerator[] idGenerators = new IdGenerator[10];
    public Set<String> tracingMethods = ConcurrentHashMap.newKeySet();

    private ResourceMethodManager() {
    }

    public void addProfilingIncludeMethod(ResourceMethod method) {
        allProfilingIncludeMethods.add(method);
        tracingMethods.add(method.getClazz().getName().replace(".", "/"));

        int order = method.getIdGenerator().getOrder();
        if (generatorIndexes.contains(order)) {
            return;
        }
        if (generatorIndexes.size() == idGenerators.length) {
            refreshIdGenerator();
        }
        addIdGenerator(method.getIdGenerator());
    }

    public ResourceMethod findProfilingIncludeMethod(String className, String methodName, String descriptor) {
        for (ResourceMethod method : allProfilingIncludeMethods) {
            if (method.getClazz().getName().equals(className) && method.getMethodName().equals(methodName) && method.getSignature().equals(descriptor)) {
                return method;
            }
        }
        return null;
    }

    private synchronized void refreshIdGenerator() {
        IdGenerator[] generators = new IdGenerator[idGenerators.length * 2];
        int i = 0;
        for (IdGenerator idGenerator : idGenerators) {
            generators[i++] = idGenerator;
        }
        idGenerators = generators;
    }

    private synchronized void addIdGenerator(IdGenerator idGenerator) {
        if (generatorIndexes.add(idGenerator.getOrder())) {
            idGenerators[generatorIndexes.size() - 1] = idGenerator;
        }
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

    public void deleteSelectedResources(String type) {
        selectedResourceTypes.remove(type);
        selectedProfilingIncludeMethods.removeIf(method -> method.getResourceType().equals(type));
    }

    public Set<String> getSelectedResourceTypes() {
        return selectedResourceTypes;
    }

    public static void destroy() {
        if (INSTANCE != null) {
            INSTANCE.generatorIndexes = null;
            INSTANCE.allProfilingIncludeMethods = null;
            INSTANCE.selectedProfilingIncludeMethods = null;
            INSTANCE.idGenerators = null;
            INSTANCE.tracingMethods = null;
            INSTANCE = null;
        }
    }

}
