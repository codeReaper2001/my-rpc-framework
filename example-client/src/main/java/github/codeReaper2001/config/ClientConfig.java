package github.codeReaper2001.config;

import github.codeReaper2001.spring.RpcReferenceFieldBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ClientConfig {
    @Bean
    public RpcReferenceFieldBeanPostProcessor getRpcReferenceFieldBeanPostProcessor() {
        // 用于填充bean的@RpcReference
        return new RpcReferenceFieldBeanPostProcessor("netty");
    }
}
