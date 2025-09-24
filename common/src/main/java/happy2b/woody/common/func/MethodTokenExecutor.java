package happy2b.woody.common.func;

import happy2b.woody.common.api.FunctionTokenExecutor;
import happy2b.woody.common.reflection.ReflectionUtils;
import happy2b.woody.common.utils.Pair;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/9/21
 */
public class MethodTokenExecutor implements FunctionTokenExecutor {

    private String name;
    private String[] params;

    private AtomicBoolean methodParsed = new AtomicBoolean(false);

    private Method method;
    private Object[] convertedParams;

    public MethodTokenExecutor(String name, String params) {
        this.name = name;
        if (params == null || params.isEmpty()) {
            this.params = new String[0];
        } else {
            this.params = params.split(",");
        }
    }

    @Override
    public Object execute(Object target) {
        if (methodParsed.compareAndSet(false, true)) {
            List<Method> methodList = ReflectionUtils.findMethodIgnoreParamTypes(target.getClass(), name);
            for (Method m : methodList) {
                Object[] tmpParams = new Object[params.length];
                Class<?>[] types = m.getParameterTypes();
                if (types.length != params.length) {
                    continue;
                }
                boolean match = true;
                for (int i = 0; i < types.length; i++) {
                    Pair<Boolean, Object> convertedParam = convert(types[i], params[i]);
                    if (convertedParam.getLeft()) {
                        tmpParams[i] = convertedParam.getRight();
                    } else {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    method = m;
                    convertedParams = tmpParams;
                    break;
                }
            }
        }
        if (method == null || convertedParams == null) {
            return null;
        }
        return ReflectionUtils.invokeMethod(method, target, convertedParams);
    }

    private Pair<Boolean, Object> convert(Class type, String token) {
        if (type == String.class) {
            if (token.equals("null")) {
                return Pair.of(true, null);
            }
            if (token.startsWith("\"") && token.endsWith("\"") || token.startsWith("\'") && token.endsWith("\'")) {
                return Pair.of(true, token.substring(1, token.length() - 1));
            }
            return Pair.of(false, token);
        }
        try {
            if (type == int.class) {
                return Pair.of(true, Integer.parseInt(token));
            } else if (type == boolean.class) {
                return Pair.of(true, Boolean.parseBoolean(token));
            } else if (type == double.class) {
                return Pair.of(true, Double.parseDouble(token));
            } else if (type == float.class) {
                return Pair.of(true, Float.parseFloat(token));
            } else if (type == long.class) {
                return Pair.of(true, Long.parseLong(token));
            } else if (type == short.class) {
                return Pair.of(true, Short.parseShort(token));
            } else if (type == byte.class) {
                return Pair.of(true, Byte.parseByte(token));
            } else if (type == char.class) {
                return Pair.of(true, token.charAt(0));
            } else if (token.equals("null")) {
                return Pair.of(true, null);
            } else if (type == Integer.class) {
                return Pair.of(true, Integer.valueOf(token));
            } else if (type == Long.class) {
                return Pair.of(true, Long.valueOf(token));
            } else if (type == Double.class) {
                return Pair.of(true, Double.valueOf(token));
            } else if (type == Float.class) {
                return Pair.of(true, Float.valueOf(token));
            } else if (type == Boolean.class) {
                return Pair.of(true, Boolean.valueOf(token));
            } else if (type == Character.class) {
                return Pair.of(true, token.charAt(0));
            } else if (type == Byte.class) {
                return Pair.of(true, Byte.valueOf(token));
            } else if (type == Short.class) {
                return Pair.of(true, Short.valueOf(token));
            }
        } catch (Exception e) {
            return Pair.of(false, token);
        }
        return Pair.of(false, token);
    }

    public Method getMethod() {
        return method;
    }
}
