package happy2b.woody.common.func;

import happy2b.woody.common.api.FunctionTokenExecutor;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

/**
 * @author jiangjibo
 * @version 1.0
 * @since 2025/9/21
 */
public class ArrayTokenExecutor implements FunctionTokenExecutor {

    private int index;

    public ArrayTokenExecutor(int index) {
        this.index = index;
    }

    @Override
    public Object execute(Object target) {
        if (target == null) {
            return null;
        }
        boolean isArray = target.getClass().isArray();
        boolean isList = List.class.isAssignableFrom(target.getClass());
        if (!isArray && !isList) {
            throw new IllegalArgumentException("Target is not an array or List: " + target);
        }
        if (isArray) {
            if (Array.getLength(target) < index) {
                throw new IllegalArgumentException("Array index out of bounds: " + index);
            }
            return Array.get(target, index);
        } else {
            if (((List) target).size() < index) {
                throw new IllegalArgumentException("List index out of bounds: " + index);
            }
            return ((List) target).get(index);
        }
    }
}
