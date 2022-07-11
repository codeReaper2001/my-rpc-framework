package github.codeReaper2001.spring;

import github.codeReaper2001.annotation.RpcService;
import github.codeReaper2001.config.RpcServiceConfig;
import github.codeReaper2001.factory.SingletonFactory;
import github.codeReaper2001.provider.ServiceProvider;
import github.codeReaper2001.provider.impl.ZkServiceProviderImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/*
* 被RpcService修饰的类的实例bean的后置处理器
* 用于自动发布服务到注册中心
* */
@Slf4j
public class RpcServiceRegisterPostProcessor implements BeanPostProcessor {
    // 用于发布服务
    private final ServiceProvider serviceProvider;

    public RpcServiceRegisterPostProcessor() {
        this.serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 检查bean对应的类是否被@RpcService注解修饰，如果是则封装成RpcServiceConfig发布服务
        if (bean.getClass().isAnnotationPresent(RpcService.class)) {
            log.info("[{}] is annotated with  [{}]", bean.getClass().getName(), RpcService.class.getCanonicalName());
            RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
            RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                    .group(rpcService.group())
                    .version(rpcService.version())
                    .service(bean).build();
            // 发布
            serviceProvider.publishService(rpcServiceConfig);
        }
        return bean;
    }
}
