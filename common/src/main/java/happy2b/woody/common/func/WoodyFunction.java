package happy2b.woody.common.func;

import happy2b.woody.common.api.FunctionTokenExecutor;
import happy2b.woody.common.reflection.ReflectionUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/9/16
 */
public class WoodyFunction {

    private static final Set<String> OPERATORS = new HashSet<>(Arrays.asList("==", "!=", ">", "<", ">=", "<="));

    private int order;
    private boolean filter;
    private String expression;

    public WoodyFunction(int order, boolean filter, String expression) {
        this.order = order;
        this.filter = filter;
        this.expression = expression;
    }

    public Object apply(Object target, FunctionTokenExecutor[] executors) {
        if (filter) {
            return executeBooleanExpression(expression, target);
        } else {
            for (FunctionTokenExecutor executor : executors) {
                target = executor.execute(target);
                if (target == null) {
                    return null;
                }
            }
            return target;
        }
    }


    public int getOrder() {
        return order;
    }

    public boolean isFilter() {
        return filter;
    }

    public String getExpression() {
        return expression;
    }

    private Object executeExtractValueExpression(String exp, Object target) {
        if (exp.startsWith("##")) {
            exp = exp.substring(2);
        }
        Object value = null;
        if (isEvalExpress(exp)) {
            value = extractNestedValue(exp, target);
        } else if (target instanceof Map) {
            value = ((Map) target).get(exp);
        } else {
            boolean exist = ReflectionUtils.existsField(target, exp);
            if (exist) {
                value = ReflectionUtils.get(target, exp);
            }
        }
        return value;
    }

    private boolean isEvalExpress(String exp) {
        return exp.contains(".") || exp.contains("[") || exp.contains("(");
    }

    private Object extractNestedValue(String exp, Object target) {
        String[] split = exp.split("\\.");
        Object obj = target;
        for (String s : split) {
            obj = extractTypesValue(s, obj);
            if (obj == null) {
                return null;
            }
        }
        return obj;
    }

    private Object extractListValue(String token, Object target) {
        token = token.trim();
        String key = token.substring(0, token.indexOf("["));
        Object value = extractTypesValue(key, target);
        if (value == null) {
            return null;
        }
        int index = Integer.valueOf(token.substring(token.indexOf("[") + 1, token.length() - 1));
        if (value.getClass().isArray() && Array.getLength(value) > index) {
            return Array.get(value, index);
        }
        if (value instanceof List && ((List) value).size() > index) {
            return ((List) value).get(index);
        }
        return null;
    }

    private Object extractTypesValue(String token, Object target) {
        if (token.contains("[")) {
            return extractListValue(token, target);
        }
        if (target instanceof Map) {
            return ((Map) target).get(token);
        }
        if (token.endsWith("(")) {
            return invokeMethodAndGetResult(token, target);
        }
        if (ReflectionUtils.existsField(target, token)) {
            return ReflectionUtils.get(target, token);
        }
        return null;
    }

    private boolean executeBooleanExpression(String exp, Object target) {
        String[] tokens = null;
        String operator = null;
        for (String opt : OPERATORS) {
            if (exp.contains(opt)) {
                tokens = exp.split(opt);
                break;
            }
        }
        if (tokens == null) {
            throw new IllegalArgumentException("Can`t find operator in expression: " + exp);
        }

        if (tokens.length != 2) {
            throw new IllegalArgumentException("Invalid expression: " + exp);
        }

        Object left = processToken(tokens[0], target);
        Object right = processToken(tokens[1], target);

        if (left == null && right != null || left != null && right == null) {
            return false;
        }
        switch (operator) {
            case "==":
                return left.equals(right);
            case "!=":
                return !left.equals(right);
            case ">":
                return ((Comparable) left).compareTo(right) > 0;
            case "<":
                return ((Comparable) left).compareTo(right) < 0;
            case ">=":
                return ((Comparable) left).compareTo(right) >= 0;
            case "<=":
                return ((Comparable) left).compareTo(right) <= 0;
            default:
                throw new RuntimeException("Unsupported operator: " + operator);
        }
    }

    private Object processToken(String token, Object target) {
        token = token.trim();
        if (token.isEmpty()) {
            return "";
        } else if (token.equals("true") || token.equals("false")) {
            return Boolean.parseBoolean(token);
        } else if (token.startsWith("##")) {
            return executeExtractValueExpression(token, target);
        } else if (token.startsWith("\"") && token.endsWith("\"") || token.startsWith("\'") && token.endsWith("\'")) {
            return token.substring(1, token.length() - 1);
        } else {
            return Double.parseDouble(token);
        }
    }

    private Object invokeMethodAndGetResult(String token, Object target) {
        String methodName = token.substring(0, token.indexOf("("));
        String params = token.substring(token.indexOf("(") + 1, token.length() - 1);
        String[] split = params.split(",");
        Class[] paramTypes = new Class[split.length];
        Object[] paramValues = new Object[split.length];
        for (int i = 0; i < split.length; i++) {
            String param = split[i].trim();
            if (param.contains("") || param.contains("'")) {
                paramTypes[i] = String.class;
                paramValues[i] = param.substring(1, param.length() - 1);
            } else if (param.equals("null")) {
                paramTypes[i] = null;
                paramValues[i] = null;
            } else {
                paramTypes[i] = Number.class;
                paramValues[i] = Double.parseDouble(param);
            }
        }
        List<Method> methodList = ReflectionUtils.findMethodIgnoreParamTypes(target.getClass(), methodName);
        for (Method method : methodList) {
            Class<?>[] types = method.getParameterTypes();
            if (types.length != paramTypes.length) {
                continue;
            }
            for (int i = 0; i < paramTypes.length; i++) {

            }
        }

        token = token.substring(0, token.length() - 2);
        return ReflectionUtils.invoke(target, token);
    }

    public FunctionTokenExecutor[] parseExpression(Class type) {
        String exp = expression;
        if (expression.startsWith("##")) {
            exp = exp.substring(2);
        }
        if (isEvalExpress(exp)) {
            List<FunctionTokenExecutor> executors = new ArrayList<>();
            String[] tokens = exp.split("\\.");
            for (String token : tokens) {
                token = token.trim();
                FunctionTokenExecutor executor = processToken(token, type);
                if (executor == null) {
                    throw new IllegalArgumentException("Invalid expression: " + exp);
                }
                executors.add(executor);
            }
            return executors.toArray(new FunctionTokenExecutor[0]);
        } else {
            return new FunctionTokenExecutor[]{new FieldOrMapTokenExecutor(exp)};
        }
    }

    private FunctionTokenExecutor processToken(String token, Class type) {
        if (token.startsWith("[") && token.endsWith("]")) {
            int index = Integer.parseInt(token.substring(1, token.length() - 1));
            return new ArrayTokenExecutor(index);
        }
        if (token.endsWith(")") && token.contains("(")) {
            String methodName = token.substring(0, token.indexOf("("));
            String params = token.substring(token.indexOf("(") + 1, token.length() - 1).trim();
            return new MethodTokenExecutor(methodName, params.split(","));
        }
        return new FieldOrMapTokenExecutor(token);
    }
}
