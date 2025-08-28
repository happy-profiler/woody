package happy2b.woody.common.reflection;


import java.lang.reflect.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ReflectionUtils {

  private static final String CGLIB_RENAMED_METHOD_PREFIX = "CGLIB$";

  private static final Method[] NO_METHODS = {};

  private static final Map<String, Field> NO_FIELDS = new ConcurrentHashMap<>();

  public static Field NOT_EXISTS_FIELD;

  private static final Map<Class<?>, Method[]> DECLARED_METHODS_CACHE =
      new ConcurrentReferenceHashMap<Class<?>, Method[]>(256);

  private static final Map<Class<?>, Map<String, Field>> DECLARED_FIELDS_CACHE =
      new ConcurrentReferenceHashMap<Class<?>, Map<String, Field>>(256);

  private static final Map<Class<?>, Constructor[]> DECLARED_CONSTRUCTOR_CACHE =
      new ConcurrentReferenceHashMap<Class<?>, Constructor[]>(256);

  public static <T> T invoke(Object target, String methodName) {
    Method method = findMethod(target.getClass(), methodName);
    return (T) invokeMethod(method, target);
  }

  public static <T> T invoke(Object target, String methodName, Object... args) {
    Class[] paramTypes = new Class[args.length];
    for (int i = 0; i < args.length; i++) {
      if (args[i] == null) {
        paramTypes[i] = NULL.class;
      } else if (args[i] instanceof Class) {
        paramTypes[i] = (Class) args[i];
      } else {
        paramTypes[i] = args[i].getClass();
      }
    }
    Method method = findMethod(target.getClass(), methodName, paramTypes);
    return (T) invokeMethod(method, target, args);
  }

  public static <T> T invokeStatic(Class clazz, String methodName, Object... args) {
    Class[] paramTypes = new Class[args.length];
    for (int i = 0; i < args.length; i++) {
      paramTypes[i] = args[i] == null ? NULL.class : args[i].getClass();
    }
    Method method = findMethod(clazz, methodName, paramTypes);
    return (T) invokeMethod(method, null, args);
  }

  public static <T> T get(Object target, String fieldName) {
    Field field = findField(target.getClass(), fieldName);
    return (T) getField(field, target);
  }

  public static void set(Object target, String fieldName, Object value) {
    Field field = findField(target.getClass(), fieldName);
    setField(field, target, value);
  }

  public static boolean setIfApplicable(Object target, String fieldName, Object value) {
    Field field = findField(target.getClass(), fieldName);
    if (field != null) {
      setField(field, target, value);
      return true;
    }
    return false;
  }

  public static <T> T newInstance(Class clazz, Object... args) {
    Class[] paramTypes = new Class[args.length];
    for (int i = 0; i < args.length; i++) {
      if (args[i] == null) {
        paramTypes[i] = NULL.class;
      } else if (args[i] instanceof Class) {
        paramTypes[i] = (Class) args[i];
      } else {
        paramTypes[i] = args[i].getClass();
      }
    }
    try {
      Constructor[] constructors = getDeclaredConstructors(clazz);
      for (Constructor constructor : constructors) {
        Class[] types = constructor.getParameterTypes();
        if (types.length != paramTypes.length) {
          continue;
        }
        boolean match = true;
        for (int i = 0; i < types.length; i++) {
          Class t1 = paramTypes[i];
          if (t1 == NULL.class) {
            continue;
          }
          if (!castToWrapperClass(types[i]).isAssignableFrom(t1)) {
            match = false;
            break;
          }
        }
        if (match) {
          return (T) constructor.newInstance(args);
        }
      }
      throw new IllegalStateException("can`t find match constructor for class :" + clazz.getName());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static <T> T newInstance(String className, ClassLoader classLoader, Object... args) {
    try {
      return newInstance(classLoader.loadClass(className), args);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static <T> T getFieldValues(Object object, String... filedNames) {
    for (String filedName : filedNames) {
      object = get(object, filedName);
    }
    return (T) object;
  }

  public static Field findField(Class<?> clazz, String name) {
    return findField(clazz, name, null);
  }

  public static Field findField(Object obj, String name) {
    Assert.notNull(obj, "Target must not be null");
    return findField(obj.getClass(), name, null);
  }

  public static boolean existsField(Class<?> clazz, String name) {
    return findField(clazz, name) != null;
  }

  public static boolean existsField(Object target, String name) {
    Assert.notNull(target, "Target must not be null");
    return findField(target.getClass(), name) != null;
  }

  public static <T> T getStatic(Class<?> clazz, String name) {
    Field field = findField(clazz, name, null);
    try {
      return (T) field.get(null);
    } catch (IllegalAccessException ex) {
      handleReflectionException(ex);
      throw new IllegalStateException(
          "Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
    }
  }

  public static void setStatic(Class<?> clazz, String name, Object value) {
    Field field = findField(clazz, name, null);
    try {
      field.setAccessible(true);
      field.set(null, value);
    } catch (IllegalAccessException ex) {
      handleReflectionException(ex);
      throw new IllegalStateException(
          "Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
    }
  }

  public static Field findField(Class<?> clazz, String name, Class<?> type) {
    Assert.notNull(clazz, "Class must not be null");
    Assert.isTrue(name != null || type != null, "Either name or type of the field must be specified");
    Class<?> searchType = clazz;
    while (Object.class != searchType && searchType != null) {
      Map<String, Field> fields = getDeclaredFields(searchType);
      Field field = fields.get(name);
      if (field != null && (type == null || type.equals(field.getType()))) {
        field.setAccessible(true);
        return field;
      }
      searchType = searchType.getSuperclass();
    }
    return null;
  }

  public static void setField(Field field, Object target, Object value) {
    try {
      field.set(target, value);
    } catch (IllegalAccessException ex) {
      handleReflectionException(ex);
      throw new IllegalStateException(
          "Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
    }
  }

  public static <T> T getField(Field field, Object target) {
    try {
      return (T) field.get(target);
    } catch (IllegalAccessException ex) {
      handleReflectionException(ex);
      throw new IllegalStateException(
          "Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
    }
  }

  public static boolean existsMethod(Class<?> clazz, String methodName) {
    return existsMethod(clazz, methodName, new Object[0]);
  }

  public static boolean existsMethod(Class<?> clazz, String methodName, Object... args) {
    Class[] paramTypes = new Class[args.length];
    for (int i = 0; i < args.length; i++) {
      paramTypes[i] = args[i] == null ? NULL.class : args[i].getClass();
    }
    return findMethod(clazz, methodName, paramTypes) != null;
  }

  public static boolean existsMethod(Class<?> clazz, String methodName, Class... classes) {
    return findMethod(clazz, methodName, classes) != null;
  }


  public static Method findMethod(Class<?> clazz, String name) {
    return findMethod(clazz, name, new Class<?>[0]);
  }

  public static List<Method> findMethodIgnoreParamTypes(Class<?> clazz, String name) {
    List<Method> candidates = new ArrayList<>();
    Class<?> searchType = clazz;
    while (searchType != null) {
      Method[] methods = (searchType.isInterface() ? searchType.getMethods() : getDeclaredMethods(searchType));
      for (Method method : methods) {
        if (name.equals(method.getName())) {
          method.setAccessible(true);
          candidates.add(method);
        }
      }
      searchType = searchType.getSuperclass();
    }
    return candidates;
  }

  public static Method findMethod(Class<?> clazz, String name, Object[] params) {
    Assert.notNull(clazz, "Class must not be null");
    Assert.notNull(name, "Method name must not be null");
    Class<?> searchType = clazz;
    while (searchType != null) {
      Method[] methods = (searchType.isInterface() ? searchType.getMethods() : getDeclaredMethods(searchType));
      for (Method method : methods) {
        if (name.equals(method.getName()) &&
            (params == null || classesEquals(params, method.getParameterTypes()))) {
          method.setAccessible(true);
          return method;
        }
      }
      searchType = searchType.getSuperclass();
    }
    return null;
  }

  public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
    Assert.notNull(clazz, "Class must not be null");
    Assert.notNull(name, "Method name must not be null");
    Class<?> searchType = clazz;
    while (searchType != null) {
      Method[] methods = (searchType.isInterface() ? searchType.getMethods() : getDeclaredMethods(searchType));
      for (Method method : methods) {
        if (name.equals(method.getName()) &&
            (paramTypes == null || classesEquals(paramTypes, method.getParameterTypes()))) {
          method.setAccessible(true);
          return method;
        }
      }
      searchType = searchType.getSuperclass();
    }
    return null;
  }

  public static <T> T invokeMethod(Method method, Object target) {
    return (T) invokeMethod(method, target, new Object[0]);
  }

  public static Object invokeMethod(Method method, Object target, Object... args) {
    try {
      return method.invoke(target, args);
    } catch (Exception ex) {
      handleReflectionException(ex);
    }
    throw new IllegalStateException("Should never get here");
  }

  public static <T> T get(Object target, String fieldName, Class<T> type) {
    Field field = findField(target.getClass(), fieldName, type);
    return (T) getField(field, target);
  }

  public static Object invokeJdbcMethod(Method method, Object target) throws SQLException {
    return invokeJdbcMethod(method, target, new Object[0]);
  }

  public static Object invokeJdbcMethod(Method method, Object target, Object... args) throws SQLException {
    try {
      return method.invoke(target, args);
    } catch (IllegalAccessException ex) {
      handleReflectionException(ex);
    } catch (InvocationTargetException ex) {
      if (ex.getTargetException() instanceof SQLException) {
        throw (SQLException) ex.getTargetException();
      }
      handleInvocationTargetException(ex);
    }
    throw new IllegalStateException("Should never get here");
  }

  public static void handleReflectionException(Exception ex) {
    if (ex instanceof NoSuchMethodException) {
      throw new IllegalStateException("Method not found: " + ex.getMessage());
    }
    if (ex instanceof IllegalAccessException) {
      throw new IllegalStateException("Could not access method: " + ex.getMessage());
    }
    if (ex instanceof InvocationTargetException) {
      handleInvocationTargetException((InvocationTargetException) ex);
    }
    if (ex instanceof RuntimeException) {
      throw (RuntimeException) ex;
    }
    throw new UndeclaredThrowableException(ex);
  }

  public static void handleInvocationTargetException(InvocationTargetException ex) {
    rethrowRuntimeException(ex.getTargetException());
  }

  public static void rethrowRuntimeException(Throwable ex) {
    if (ex instanceof RuntimeException) {
      throw (RuntimeException) ex;
    }
    if (ex instanceof Error) {
      throw (Error) ex;
    }
    throw new UndeclaredThrowableException(ex);
  }

  public static boolean isCglibRenamedMethod(Method renamedMethod) {
    String name = renamedMethod.getName();
    if (name.startsWith(CGLIB_RENAMED_METHOD_PREFIX)) {
      int i = name.length() - 1;
      while (i >= 0 && Character.isDigit(name.charAt(i))) {
        i--;
      }
      return ((i > CGLIB_RENAMED_METHOD_PREFIX.length()) &&
          (i < name.length() - 1) && name.charAt(i) == '$');
    }
    return false;
  }

  public static void makeAccessible(Field field) {
    if ((!Modifier.isPublic(field.getModifiers()) ||
        !Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
        Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
      field.setAccessible(true);
    }
  }

  public static Field findFieldByPrefix(Class clazz, String namePrefix) {
    Assert.notNull(clazz, "Class must not be null");
    Class<?> searchType = clazz;
    while (Object.class != searchType && searchType != null) {
      Map<String, Field> fields = getDeclaredFields(searchType);
      for (Map.Entry<String, Field> entry : fields.entrySet()) {
        String name = entry.getKey();
        if (name.startsWith(namePrefix)) {
          entry.getValue().setAccessible(true);
          return entry.getValue();
        }
      }
      searchType = searchType.getSuperclass();
    }
    return null;
  }

  public static void doWithLocalMethods(Class<?> clazz, MethodCallback mc) {
    Method[] methods = getDeclaredMethods(clazz);
    for (Method method : methods) {
      try {
        mc.doWith(method);
      } catch (IllegalAccessException ex) {
        throw new IllegalStateException("Not allowed to access method '" + method.getName() + "': " + ex);
      }
    }
  }

  public static void doWithMethods(Class<?> clazz, MethodCallback mc) {
    doWithMethods(clazz, mc, null);
  }

  public static void doWithMethods(Class<?> clazz, MethodCallback mc, MethodFilter mf) {
    // Keep backing up the inheritance hierarchy.
    Method[] methods = getDeclaredMethods(clazz);
    for (Method method : methods) {
      if (mf != null && !mf.matches(method)) {
        continue;
      }
      try {
        mc.doWith(method);
      } catch (IllegalAccessException ex) {
        throw new IllegalStateException("Not allowed to access method '" + method.getName() + "': " + ex);
      }
    }
    if (clazz.getSuperclass() != null) {
      doWithMethods(clazz.getSuperclass(), mc, mf);
    } else if (clazz.isInterface()) {
      for (Class<?> superIfc : clazz.getInterfaces()) {
        doWithMethods(superIfc, mc, mf);
      }
    }
  }

  public static Method[] getAllDeclaredMethods(Class<?> leafClass) {
    final List<Method> methods = new ArrayList<Method>(32);
    doWithMethods(leafClass, new MethodCallback() {
      @Override
      public void doWith(Method method) {
        methods.add(method);
      }
    });
    return methods.toArray(new Method[methods.size()]);
  }

  public static Method[] getUniqueDeclaredMethods(Class<?> leafClass) {
    final List<Method> methods = new ArrayList<Method>(32);
    doWithMethods(leafClass, new MethodCallback() {
      @Override
      public void doWith(Method method) {
        boolean knownSignature = false;
        Method methodBeingOverriddenWithCovariantReturnType = null;
        for (Method existingMethod : methods) {
          if (method.getName().equals(existingMethod.getName()) && classesEquals(method.getParameterTypes(), existingMethod.getParameterTypes())) {
            // Is this a covariant return type situation?
            if (existingMethod.getReturnType() != method.getReturnType() &&
                existingMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
              methodBeingOverriddenWithCovariantReturnType = existingMethod;
            } else {
              knownSignature = true;
            }
            break;
          }
        }
        if (methodBeingOverriddenWithCovariantReturnType != null) {
          methods.remove(methodBeingOverriddenWithCovariantReturnType);
        }
        if (!knownSignature && !isCglibRenamedMethod(method)) {
          methods.add(method);
        }
      }
    });
    return methods.toArray(new Method[methods.size()]);
  }

  public static Method[] getDeclaredMethods(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    Method[] result = DECLARED_METHODS_CACHE.get(clazz);
    if (result == null) {
      Method[] declaredMethods = clazz.getDeclaredMethods();
      List<Method> defaultMethods = findConcreteMethodsOnInterfaces(clazz);
      if (defaultMethods != null) {
        result = new Method[declaredMethods.length + defaultMethods.size()];
        System.arraycopy(declaredMethods, 0, result, 0, declaredMethods.length);
        int index = declaredMethods.length;
        for (Method defaultMethod : defaultMethods) {
          result[index] = defaultMethod;
          index++;
        }
      } else {
        result = declaredMethods;
      }
      DECLARED_METHODS_CACHE.put(clazz, (result.length == 0 ? NO_METHODS : result));
    }
    return result;
  }

  private static <T extends Member> void copyToMap(T[] array, Map<String, T> map) {
    for (T t : array) {
      map.put(t.getName(), t);
    }
  }

  private static <T extends Member> void copyToMap(List<T> array, Map<String, T> map) {
    for (T t : array) {
      map.put(t.getName(), t);
    }
  }

  private static <T extends Member> Map<String, T> toMap(T[] array) {
    Map<String, T> elements = new ConcurrentHashMap<>();
    for (T t : array) {
      elements.put(t.getName(), t);
    }
    return elements;
  }

  private static List<Method> findConcreteMethodsOnInterfaces(Class<?> clazz) {
    List<Method> result = null;
    for (Class<?> ifc : clazz.getInterfaces()) {
      for (Method ifcMethod : ifc.getMethods()) {
        if (!Modifier.isAbstract(ifcMethod.getModifiers())) {
          if (result == null) {
            result = new ArrayList<Method>();
          }
          result.add(ifcMethod);
        }
      }
    }
    return result;
  }

  public static void doWithLocalFields(Class<?> clazz, FieldCallback fc) {
    for (Map.Entry<String, Field> entry : getDeclaredFields(clazz).entrySet()) {
      Field field = entry.getValue();
      try {
        fc.doWith(field);
      } catch (IllegalAccessException ex) {
        throw new IllegalStateException("Not allowed to access field '" + field.getName() + "': " + ex);
      }
    }
  }

  private static void notExists() {
  }

  public static void doWithFields(Class<?> clazz, FieldCallback fc) {
    doWithFields(clazz, fc, null);
  }

  public static void doWithFields(Class<?> clazz, FieldCallback fc, FieldFilter ff) {
    // Keep backing up the inheritance hierarchy.
    Class<?> targetClass = clazz;
    do {
      Map<String, Field> fields = getDeclaredFields(targetClass);
      for (Map.Entry<String, Field> entry : fields.entrySet()) {
        if (ff != null && !ff.matches(entry.getValue())) {
          continue;
        }
        try {
          fc.doWith(entry.getValue());
        } catch (IllegalAccessException ex) {
          throw new IllegalStateException("Not allowed to access field '" + entry.getValue().getName() + "': " + ex);
        }
      }
      targetClass = targetClass.getSuperclass();
    }
    while (targetClass != null && targetClass != Object.class);
  }

  private static Map<String, Field> getDeclaredFields(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    Map<String, Field> result = DECLARED_FIELDS_CACHE.get(clazz);
    if (result == null) {
      Field[] fields = clazz.getDeclaredFields();
      DECLARED_FIELDS_CACHE.put(clazz, result = (fields.length == 0 ? NO_FIELDS : toMap(fields)));
    }
    return result;
  }

  private static Constructor[] getDeclaredConstructors(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    Constructor[] result = DECLARED_CONSTRUCTOR_CACHE.get(clazz);
    if (result == null) {
      result = clazz.getDeclaredConstructors();
      for (Constructor constructor : result) {
        constructor.setAccessible(true);
      }
      DECLARED_CONSTRUCTOR_CACHE.put(clazz, result);
    }
    return result;
  }

  public static void clearCache() {
    DECLARED_METHODS_CACHE.clear();
    DECLARED_FIELDS_CACHE.clear();
  }

  public interface MethodCallback {

    void doWith(Method method) throws IllegalArgumentException, IllegalAccessException;
  }

  public interface MethodFilter {

    boolean matches(Method method);
  }

  public interface FieldCallback {

    void doWith(Field field) throws IllegalArgumentException, IllegalAccessException;
  }

  public interface FieldFilter {

    boolean matches(Field field);
  }

  public static final MethodFilter NON_BRIDGED_METHODS = new MethodFilter() {

    @Override
    public boolean matches(Method method) {
      return !method.isBridge();
    }
  };

  public static final MethodFilter USER_DECLARED_METHODS = new MethodFilter() {

    @Override
    public boolean matches(Method method) {
      return (!method.isBridge() && method.getDeclaringClass() != Object.class);
    }
  };

  public static final FieldFilter COPYABLE_FIELDS = new FieldFilter() {

    @Override
    public boolean matches(Field field) {
      return !(Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers()));
    }
  };

  private static boolean classesEquals(Object[] obj1, Class[] cls2) {
    if (obj1 == cls2)
      return true;
    if (obj1 == null || cls2 == null)
      return false;

    int length = obj1.length;
    if (cls2.length != length) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      Object o1 = obj1[i];
      if (o1 == null) {
        continue;
      }
      Class c1 = o1.getClass();
      Class c2 = cls2[i];
      if (o1.equals(c2) || c1.isAssignableFrom(c2) || c2.isAssignableFrom(c1)) {
        continue;
      }
      if (castToWrapperClass(c1) == castToWrapperClass(c2)) {
        continue;
      }
      return false;
    }

    return true;
  }

  private static boolean classesEquals(Class[] cls1, Class[] cls2) {
    if (cls1 == cls2)
      return true;
    if (cls1 == null || cls2 == null)
      return false;

    int length = cls1.length;
    if (cls2.length != length)
      return false;

    for (int i = 0; i < length; i++) {
      Class c1 = cls1[i];
      Class c2 = cls2[i];
      if (c1 == NULL.class || c2 == NULL.class) {
        continue;
      }
      if (c1.equals(c2) || c1.isAssignableFrom(c2) || c2.isAssignableFrom(c1)) {
        continue;
      }
      if (castToWrapperClass(c1) == castToWrapperClass(c2)) {
        continue;
      }
      return false;
    }

    return true;
  }

  private static Class castToWrapperClass(Class clazz) {
    if (clazz == boolean.class) {
      return Boolean.class;
    }
    if (clazz == byte.class) {
      return Byte.class;
    }
    if (clazz == int.class) {
      return Integer.class;
    }
    if (clazz == long.class) {
      return Long.class;
    }
    if (clazz == double.class) {
      return Double.class;
    }
    if (clazz == float.class) {
      return Float.class;
    }
    if (clazz == short.class) {
      return Short.class;
    }
    if (clazz == char.class) {
      return Character.class;
    }
    return clazz;
  }

  public static void destroy() {
    DECLARED_METHODS_CACHE.clear();
    DECLARED_FIELDS_CACHE.clear();
    DECLARED_CONSTRUCTOR_CACHE.clear();
  }

  private static class NULL {
  }

}
