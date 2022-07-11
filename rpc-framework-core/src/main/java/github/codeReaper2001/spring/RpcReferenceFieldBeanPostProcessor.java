package github.codeReaper2001.spring;

import github.codeReaper2001.annotation.RpcReference;
import github.codeReaper2001.config.RpcServiceConfig;
import github.codeReaper2001.extension.ExtensionLoader;
import github.codeReaper2001.proxy.RpcClientProxy;
import github.codeReaper2001.remoting.transport.RpcRequestTransport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;

/*
* 创建rpc客户端代理填充
* bean中被@RpcReference修饰的字段
* */
public class RpcReferenceFieldBeanPostProcessor implements BeanPostProcessor {

    // 用于发送rpc请求
    private final RpcRequestTransport rpcClient;

    public RpcReferenceFieldBeanPostProcessor(String clientExtensionName) {
        this.rpcClient = ExtensionLoader.getExtensionLoader(RpcRequestTransport.class)
                .getExtension(clientExtensionName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        /*
        * 1. 获取class对象
        * 2. 遍历所有字段，如果该字段被@RpcReference注解修饰，则生成代理对象填充到该字段上
        * */
        Class<?> targetClass = bean.getClass();
        Field[] fields = targetClass.getDeclaredFields();
        for (Field field : fields) {
            // 获取字段上的@RpcReference注解
            RpcReference rpcReference = field.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                        .group(rpcReference.group())
                        .version(rpcReference.version()).build();
                // 生成代理对象
                RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient, rpcServiceConfig);
                Object serviceProxy = rpcClientProxy.getProxy(field.getType());
                field.setAccessible(true);
                // 设置到字段上
                try {
                    field.set(bean, serviceProxy);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }
}
