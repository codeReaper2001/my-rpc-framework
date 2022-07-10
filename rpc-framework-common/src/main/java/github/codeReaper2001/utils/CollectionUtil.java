package github.codeReaper2001.utils;

import java.util.Collection;

public class CollectionUtil {

    // 判断容器是否为空
    public static boolean isEmpty(Collection<?> c) {
        return c == null || c.isEmpty();
    }

}
