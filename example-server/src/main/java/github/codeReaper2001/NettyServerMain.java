package github.codeReaper2001;

import github.codeReaper2001.config.RpcServiceConfig;
import github.codeReaper2001.remoting.transport.netty.server.NettyRpcServer;
import github.codeReaper2001.serviceimpl.MyServiceImpl;

public class NettyServerMain {
    public static void main(String[] args) {
        NettyRpcServer nettyRpcServer = new NettyRpcServer();
        // 手动创建服务实例
        MyService myService = new MyServiceImpl();
        RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                .group("test").version("version1").service(myService).build();
        nettyRpcServer.registerService(rpcServiceConfig);
        nettyRpcServer.start();
    }
}
