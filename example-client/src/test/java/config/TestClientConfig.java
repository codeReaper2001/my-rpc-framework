package config;

import github.codeReaper2001.spring.RpcReferenceFieldBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

// 扫描创建@Component bean
@ComponentScan(basePackages = "controller")
public class TestClientConfig {
    @Bean
    public RpcReferenceFieldBeanPostProcessor getRpcReferenceFieldBeanPostProcessor() {
        // 用于填充bean的@RpcReference
        return new RpcReferenceFieldBeanPostProcessor("netty");
    }
}
