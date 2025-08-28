package happy2b.woody.common.utils;

import happy2b.woody.common.reflection.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class MethodUtil {

  public static String getMethodDescriptor(final Method method) {
    return getMethodDescriptor("", method);
  }

  public static String getMethodDescriptor(String prefix, final Method method) {
    StringBuilder stringBuilder = new StringBuilder(prefix);
    stringBuilder.append('(');
    Class<?>[] parameters = method.getParameterTypes();
    for (Class<?> parameter : parameters) {
      appendDescriptor(parameter, stringBuilder);
    }
    stringBuilder.append(')');
    appendDescriptor(method.getReturnType(), stringBuilder);
    return stringBuilder.toString();
  }

  public static String getFullMethodDescriptor(final Method method) {
    StringBuilder sb = new StringBuilder();
    String className = method.getDeclaringClass().getName();
    sb.append(className).append(".");
    String name = method.getName();
    sb.append(name);
    return getMethodDescriptor(sb.toString(), method);
  }

  public static boolean returnPrimitiveWrapType(Method method) {
    String descriptor = getMethodDescriptor(method);
    return descriptor.endsWith("java/lang/Integer;") || descriptor.endsWith("java/lang/Boolean;") || descriptor.endsWith("java/lang/Byte;") || descriptor.endsWith("java/lang/Character;")
        || descriptor.endsWith("java/lang/Short;") || descriptor.endsWith("java/lang/Double;") || descriptor.endsWith("java/lang/Float;") || descriptor.endsWith("java/lang/Long;");
  }

  private static void appendDescriptor(final Class<?> clazz, final StringBuilder stringBuilder) {
    Class<?> currentClass = clazz;
    while (currentClass.isArray()) {
      stringBuilder.append('[');
      currentClass = currentClass.getComponentType();
    }
    if (currentClass.isPrimitive()) {
      char descriptor;
      if (currentClass == Integer.TYPE) {
        descriptor = 'I';
      } else if (currentClass == Void.TYPE) {
        descriptor = 'V';
      } else if (currentClass == Boolean.TYPE) {
        descriptor = 'Z';
      } else if (currentClass == Byte.TYPE) {
        descriptor = 'B';
      } else if (currentClass == Character.TYPE) {
        descriptor = 'C';
      } else if (currentClass == Short.TYPE) {
        descriptor = 'S';
      } else if (currentClass == Double.TYPE) {
        descriptor = 'D';
      } else if (currentClass == Float.TYPE) {
        descriptor = 'F';
      } else if (currentClass == Long.TYPE) {
        descriptor = 'J';
      } else {
        throw new AssertionError();
      }
      stringBuilder.append(descriptor);
    } else {
      stringBuilder.append('L').append(getInternalName(currentClass)).append(';');
    }
  }

  private static String getInternalName(final Class<?> clazz) {
    return clazz.getName().replace('.', '/');
  }

  public static Method findTransformTargetMethod(Object target, String field, String methodName, String desc) {
    try {
      Object element = ReflectionUtils.get(target, field);
      if (Proxy.isProxyClass(element.getClass())) {
        WoodyLog.error("Can`t find target Method for JDK Proxy class {}", element.getClass().getName());
        return null;
      }
      Class targetClass = element.getClass();
      if (targetClass.getName().contains("$$EnhancerBySpringCGLIB$$")) {
        targetClass = targetClass.getSuperclass();
      }
      List<Method> methods = ReflectionUtils.findMethodIgnoreParamTypes(targetClass, methodName);
      for (Method method : methods) {
        if (desc.equals(MethodUtil.getMethodDescriptor("", method))) {
          return method;
        }
      }
      WoodyLog.error("Failed find target method {} with desc {} from class {}", methodName, desc, targetClass.getName());
      return null;
    } catch (Exception e) {
      WoodyLog.error("Find transform target method for target {} field {} method {} desc {} occur exception!", target.getClass().getName(), field, methodName, desc);
      return null;
    }
  }



}
