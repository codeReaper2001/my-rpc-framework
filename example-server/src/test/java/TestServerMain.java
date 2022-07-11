import github.codeReaper2001.MyService;
import github.codeReaper2001.config.RpcServiceConfig;
import github.codeReaper2001.config.ServerConfig;
import github.codeReaper2001.remoting.transport.netty.server.NettyRpcServer;
import github.codeReaper2001.serviceimpl.MyServiceImpl;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TestServerMain {
    public static void main(String[] args) {
        // 设置日志打印等级
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "Error");

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
