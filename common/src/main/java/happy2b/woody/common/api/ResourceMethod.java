package happy2b.woody.common.api;


import happy2b.woody.common.id.CustomizeIdGenerator;
import happy2b.woody.common.id.ThreadLocalRandomIdGenerator;
import happy2b.woody.common.id.TimeBasedIdGenerator;
import happy2b.woody.common.utils.MethodUtil;
import happy2b.woody.common.id.IdGenerator;

import java.lang.reflect.Method;
import java.sql.Time;
import java.util.Objects;

public class ResourceMethod {
    private int order;
    private Class clazz;
    private String methodName;
    private String descriptor;

    private Method method;

    private String methodPath;
    /**
     * http, dubbo, task等
     */
    private String resourceType;
    private String resource;

    private IdGenerator idGenerator = TimeBasedIdGenerator.INSTANCE;
    private FunctionTokenExecutor[] functionTokenExecutors;

    public ResourceMethod(String resourceType, String resource, Method method) {
        this.resourceType = resourceType;
        this.resource = resource;
        this.methodName = method.getName();
        this.method = method;
        this.methodPath = method.getDeclaringClass().getName() + "." + method.getName();
        this.clazz = method.getDeclaringClass();
        this.descriptor = MethodUtil.getMethodDescriptor(method);
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Class getClazz() {
        return clazz;
    }


    public String getMethodName() {
        return methodName;
    }


    public String getSignature() {
        return descriptor;
    }


    public String getResourceType() {
        return resourceType;
    }

    public void setIdGenerator(IdGenerator idGenerator) {
        this.functionTokenExecutors = null;
        if (idGenerator instanceof CustomizeIdGenerator) {
            this.functionTokenExecutors = parseExpression((CustomizeIdGenerator) idGenerator);
        }
        this.idGenerator = idGenerator;
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    public String getResource() {
        return resource;
    }

    public Method getMethod() {
        return method;
    }

    public String getMethodPath() {
        return methodPath;
    }

    public FunctionTokenExecutor[] getFunctionTokenExecutors() {
        return functionTokenExecutors;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ResourceMethod that = (ResourceMethod) o;
        return order == that.order && Objects.equals(clazz, that.clazz) && Objects.equals(methodName, that.methodName) && Objects.equals(descriptor, that.descriptor) && Objects.equals(method, that.method) && Objects.equals(methodPath, that.methodPath) && Objects.equals(resourceType, that.resourceType) && Objects.equals(resource, that.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(order, clazz, methodName, descriptor, method, methodPath, resourceType, resource);
    }

    private FunctionTokenExecutor[] parseExpression(CustomizeIdGenerator idGenerator) {
        int index = idGenerator.paramIndex();
        Class<?>[] types = method.getParameterTypes();
        if (types.length < index) {
            throw new IllegalArgumentException("Invalid param index: " + index + " for method: " + method);
        }
        Class paramType = types[index];
        return idGenerator.getFunction().parseExpression(paramType);
    }
}
