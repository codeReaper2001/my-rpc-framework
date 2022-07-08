package github.codeReaper2001.extension;

import github.codeReaper2001.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class ExtensionLoader<T> {

    // SERVICE搜索路径
    private static final String SERVICE_DIRECTORY = "META-INF/extensions/";
    // 一级map：接口Class对象 -> ExtensionLoader实例
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();
    // 存放所有服务实例：具体类Class对象 -> 创建出来的实例
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    // 接口Class对象，表示当前ExtensionLoader管理的是什么接口下的服务
    private final Class<?> type;
    // 存放的映射：服务名称 -> 服务实例
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();
    // value存放的映射：服务名称 -> 服务类对象
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    /*
    * 例如 META-INF/extensions/ 下一个文件的内容为：
    * kyro=github.javaguide.serialize.kyro.KryoSerializer
    * protostuff=github.javaguide.serialize.protostuff.ProtostuffSerializer
    * hessian=github.javaguide.serialize.hessian.HessianSerializer
    * 则 cachedClasses.value()，即Map<String, Class<?>>存放的是：
    * {"kyro": KryoSerializer对应的class, "protostuff": ProtostuffSerializer对应的class, "hessian": HessianSerializer对应的class}
    * 而Map<String, Holder<Object>> cachedInstances则相当于将上面的value变成对应的对象实例
    * */

    // 构造函数为private类型，只能使用类方法getExtensionLoader进行构造或获取
    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }

    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type) {
        // 参数为null
        if (type == null) {
            throw new IllegalArgumentException("Extension type should not be null.");
        }
        // 参数必须为接口类型的Class实例，否则报错
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type must be an interface.");
        }
        // 对应接口类型应该要被SPI注解修饰，否则无效
        if (type.getAnnotation(SPI.class) == null) {
            throw new IllegalArgumentException("Extension type must be annotated by @SPI");
        }
        /*
        * 综上，传入的参数需要是：
        * ① 接口类型的Class实例
        * ② 该接口被SPI注解修饰
        * */
        // firstly get from cache, if not hit, create one
        // 即先查看这个 接口 对应的 ExtensionLoader 是否已经存在
        ExtensionLoader<S> extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        if (extensionLoader == null) {
            // 不存在，则创建
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<>(type));
            extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        }
        return extensionLoader;
    }

    // 根据服务名获取服务对象
    public T getExtension(String name) {
        if (StringUtil.isBlank(name)) {
            throw new IllegalArgumentException("Extension name should not be null or empty.");
        }
        // 首先从cache中查询服务是否已存在
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }

        // 服务实例不存在时则先创建实例
        // double check 保证线程安全
        // 个人理解 Holder 的作用是用来作为 synchronized 的锁对象，实现懒加载的目的
        Object serviceInstance = holder.get();
        if (serviceInstance == null) {
            synchronized (holder) {
                serviceInstance = holder.get();
                if (serviceInstance == null) {
                    serviceInstance = createExtension(name);
                    holder.set(serviceInstance);
                }
            }
        }
        return (T) serviceInstance;
    }

    private T createExtension(String name) {
        // 获取服务名 -> 服务Class对象 的map集合，且如果未加载则先使用类加载器加载
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw new RuntimeException("No such extension of name " + name);
        }
        // 从服务实例集合(cache)中寻找服务，如果没有则使用clazz创建服务实例，
        // 并保存到EXTENSION_INSTANCES中
        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        if (instance == null) {
            try {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    /*
    * 获取 服务名 -> 服务Class对象 的map集合
    * */
    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        // double check 双重检查
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = new HashMap<>();
                    // load all extensions from our extensions directory
                    // 将所有extensions路径下的当前接口文件对应的class对象注册进来（类加载后put到map中）
                    loadDirectory(classes);
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }


    private void loadDirectory(Map<String, Class<?>> extensionClasses) {
        // 文件名为 SERVICE_DIRECTORY 目录下的当前ExtensionLoader负责的 接口类型全限定名
        // 例如：META-INF/extensions/github.javaguide.serialize.Serializer
        String fileName = ExtensionLoader.SERVICE_DIRECTORY + type.getName();
        try {
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            Enumeration<URL> urls = classLoader.getResources(fileName);
            if (urls != null) {
                // 将文件中的：服务名 -> 服务类类名
                // 加载到extensionClasses中：服务名 -> 服务类类对象
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    loadResource(extensionClasses, classLoader, url);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL resourceUrl) {
        // 读取文件
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream(), UTF_8))) {
            String line;
            // read every line
            while ((line = reader.readLine()) != null) {
                // get index of comment
                final int ci = line.indexOf('#');
                if (ci >= 0) {
                    // string after # is comment so we ignore it
                    line = line.substring(0, ci);
                }
                line = line.trim();
                if (line.length() > 0) {
                    try {
                        // 等号左边的为服务名，右边的为服务类类名
                        final int ei = line.indexOf('=');
                        String name = line.substring(0, ei).trim();
                        String clazzName = line.substring(ei + 1).trim();
                        // our SPI use key-value pair so both of them must not be empty
                        if (name.length() > 0 && clazzName.length() > 0) {
                            // 使用类加载器将类加载到内存中
                            Class<?> clazz = classLoader.loadClass(clazzName);
                            // 注册到extensionClasses中
                            extensionClasses.put(name, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.error(e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
