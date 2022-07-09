package github.codeReaper2001.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 获取单例对象的工厂类
 */
public class SingletonFactory {
    private static final Map<Class<?>, Object> OBJECT_MAP = new ConcurrentHashMap<>();

    private SingletonFactory() {
    }

    public static <T> T getInstance(Class<T> c) {
        if (c == null) {
            throw new IllegalArgumentException();
        }
        // 如果实例已存在，则直接返回
        if (OBJECT_MAP.containsKey(c)) {
            return c.cast(OBJECT_MAP.get(c));
        } else {
            // 如果不存在，则使用反射创建对象并返回
            // 这里使用了computeIfAbsent方法保证了线程安全（只有一个线程可以执行创建对象的逻辑）
            return c.cast(OBJECT_MAP.computeIfAbsent(c, clazz -> {
                try {
                    return c.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }));
        }
    }

}
