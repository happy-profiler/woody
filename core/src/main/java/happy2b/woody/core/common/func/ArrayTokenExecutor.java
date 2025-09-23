package happy2b.woody.core.common.func;

import happy2b.woody.common.api.FunctionTokenExecutor;

import java.lang.reflect.Array;

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
        if (!target.getClass().isArray()) {
            throw new IllegalArgumentException("Target is not an array: " + target);
        }
        return Array.get(target, index);
    }
}
