package github.codeReaper2001;

import github.codeReaper2001.config.RpcServiceConfig;
import github.codeReaper2001.config.ServerConfig;
import github.codeReaper2001.remoting.transport.netty.server.NettyRpcServer;
import github.codeReaper2001.serviceimpl.MyServiceImpl;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class NettyServerMain {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ServerConfig.class);
        NettyRpcServer nettyRpcServer = applicationContext.getBean(NettyRpcServer.class);
        // 手动创建并发布服务实例
        MyService myService = new MyServiceImpl();
        RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                .group("test").version("version1").service(myService).build();
        nettyRpcServer.registerService(rpcServiceConfig);
        // 启动服务器
        nettyRpcServer.start();
    }
}
