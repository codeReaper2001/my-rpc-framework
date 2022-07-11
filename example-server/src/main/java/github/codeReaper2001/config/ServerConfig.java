package github.codeReaper2001.config;

import github.codeReaper2001.annotation.RpcScan;
import github.codeReaper2001.remoting.transport.netty.server.NettyRpcServer;
import github.codeReaper2001.spring.RpcServiceRegisterPostProcessor;
import org.springframework.context.annotation.Bean;

// 扫描并为下面包下的被@RpcService修饰的类创建bean实例
@RpcScan(basePackage = "github.codeReaper2001.serviceimpl")
public class ServerConfig {
    @Bean
    public RpcServiceRegisterPostProcessor getRpcServiceBeanPostProcessor() {
        // 用于自动将@RpcService bean注册到注册中心
        return new RpcServiceRegisterPostProcessor();
    }

    @Bean
    public NettyRpcServer getNettyRpcServer() {
        // 服务器对象添加到IOC容器
        return new NettyRpcServer();
    }
}
