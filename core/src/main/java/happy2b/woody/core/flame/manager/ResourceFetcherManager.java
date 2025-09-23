package happy2b.woody.core.flame.manager;

import happy2b.woody.common.api.id.IdGenerator;
import happy2b.woody.common.bytecode.InstrumentationUtils;
import happy2b.woody.core.flame.resource.ResourceMethod;
import happy2b.woody.core.flame.resource.fetch.ResourceFetcher;
import happy2b.woody.core.flame.resource.fetch.plugin.*;

import java.util.*;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/8/21
 */
public class ResourceFetcherManager {

    public static ResourceFetcherManager INSTANCE = new ResourceFetcherManager();

    private List<String> allAvailableResourceTypes;
    private Map<String, ResourceFetcher> allResourceFetchers = new HashMap<>();

    private ResourceFetcherManager() {
        addResourceFetcher(SpringWebResourceFetcher.INSTANCE);
        addResourceFetcher(GrpcResourceFetcher.INSTANCE);
        addResourceFetcher(DubboResourceFetcher.INSTANCE);
        addResourceFetcher(RocketMQResourceFetcher.INSTANCE);
        addResourceFetcher(KafkaResourceFetcher.INSTANCE);
    }

    private void addResourceFetcher(ResourceFetcher fetcher) {
        allResourceFetchers.put(fetcher.resourceType().getValue(), fetcher);
    }

    public void deleteSelectedResources(String type, Integer... orders) {
        ResourceMethodManager.INSTANCE.deleteSelectedResources(type, orders);
    }

    public Set<String> listSelectedResourceTypes() {
        return ResourceMethodManager.INSTANCE.getSelectedResourceTypes();
    }

    public void selectResources(IdGenerator idGenerator, String... resourceTypes) {
        for (String resourceType : resourceTypes) {
            ResourceFetcher fetcher = allResourceFetchers.get(resourceType);
            if (fetcher == null) {
                throw new IllegalArgumentException("resourceType is not valid: " + resourceType);
            }
            Set<ResourceMethod> methods = ResourceMethodManager.INSTANCE.getResourceByType(resourceType);
            if (methods == null) {
                List<Class> classList = InstrumentationUtils.findClass(fetcher.getResourceClassName());
                if (!classList.isEmpty()) {
                    for (Class clazz : classList) {
                        fetcher.fetchResources(clazz);
                    }
                }
                methods = ResourceMethodManager.INSTANCE.getResourceByType(resourceType);
            }

            for (ResourceMethod method : methods) {
                method.setIdGenerator(idGenerator);
            }

            ResourceMethodManager.INSTANCE.addSelectedResourceMethod(methods);
        }
    }

    public void selectResources(IdGenerator idGenerator, String resourceType, List<Integer> orders) {
        if (!allResourceFetchers.containsKey(resourceType)) {
            throw new IllegalArgumentException("resourceType is not valid: " + resourceType);
        }
        Set<ResourceMethod> methods = ResourceMethodManager.INSTANCE.getResourceByType(resourceType);
        for (ResourceMethod method : methods) {
            for (Integer order : orders) {
                if (method.getOrder() == order) {
                    method.setIdGenerator(idGenerator);
                    ResourceMethodManager.INSTANCE.addSelectedResourceMethod(method);
                }
            }
        }
    }

    public List<String> listAllAvailableResourceTypes() {
        if (allAvailableResourceTypes != null) {
            return allAvailableResourceTypes;
        }
        List<String> classes = new ArrayList<>();
        for (ResourceFetcher value : allResourceFetchers.values()) {
            for (String clazz : value.getResourceClassName()) {
                classes.add(clazz);
            }
        }
        List<String> types = new ArrayList<>();
        List<Class> selectClasses = InstrumentationUtils.findClass(classes.toArray(new String[0]));
        for (Class selectClass : selectClasses) {
            for (ResourceFetcher fetcher : allResourceFetchers.values()) {
                if (fetcher.isSupport(selectClass)) {
                    types.add(fetcher.resourceType().getValue());
                    break;
                }
            }
        }
        allAvailableResourceTypes = Collections.unmodifiableList(types);
        return types;
    }

    public Set<ResourceMethod> listSelectedResources(String resourceType) {
        return ResourceMethodManager.INSTANCE.getSelectedResourceByType(resourceType);
    }

    public Map<String, Set<ResourceMethod>> listAllSelectedResources() {
        if (allAvailableResourceTypes == null) {
            listAllAvailableResourceTypes();
        }
        Map<String, Set<ResourceMethod>> result = new HashMap<>();
        for (String resourceType : allAvailableResourceTypes) {
            Set<ResourceMethod> methods = listSelectedResources(resourceType);
            if (!methods.isEmpty()) {
                result.put(resourceType, methods);
            }
        }
        return result;
    }

    public Set<ResourceMethod> listResources(String resourceType) {
        ResourceFetcher fetcher = allResourceFetchers.get(resourceType);
        Set<ResourceMethod> methods = ResourceMethodManager.INSTANCE.getResourceByType(resourceType);
        if (methods.isEmpty()) {
            List<Class> classList = InstrumentationUtils.findClass(fetcher.getResourceClassName());
            if (!classList.isEmpty()) {
                for (Class clazz : classList) {
                    fetcher.fetchResources(clazz);
                }
            }
            methods = ResourceMethodManager.INSTANCE.getResourceByType(resourceType);
        }
        return methods;
    }

    public Map<String, Set<ResourceMethod>> listAllResources() {
        if (allAvailableResourceTypes == null) {
            listAllAvailableResourceTypes();
        }
        Map<String, Set<ResourceMethod>> result = new HashMap<>();
        for (String resourceType : allAvailableResourceTypes) {
            Set<ResourceMethod> methods = listResources(resourceType);
            if (!methods.isEmpty()) {
                result.put(resourceType, methods);
            }
        }
        return result;
    }

    public static void destroy() {
        if (INSTANCE != null) {
            INSTANCE.allResourceFetchers = null;
            INSTANCE.allAvailableResourceTypes = null;
            INSTANCE = null;
        }
    }


}
