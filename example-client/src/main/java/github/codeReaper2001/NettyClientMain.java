package github.codeReaper2001;

import github.codeReaper2001.config.RpcServiceConfig;
import github.codeReaper2001.proxy.RpcClientProxy;
import github.codeReaper2001.remoting.transport.netty.client.NettyRpcClient;
import lombok.extern.slf4j.Slf4j;

// 当前版本用于测试，还未编写完成
@Slf4j
public class NettyClientMain {

    public static void main(String[] args) {
        NettyRpcClient nettyRpcClient = new NettyRpcClient();

        RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                .group("test")
                .version("version1").build();
        // 手动创建代理对象，后面添加注解实现
        RpcClientProxy rpcClientProxy = new RpcClientProxy(nettyRpcClient, rpcServiceConfig);
        MyService myService = rpcClientProxy.getProxy(MyService.class);
        // 使用代理对象完成远程过程调用
        String result = myService.hello(new Data("jack", 15));
        System.out.println("result: " + result);
    }
}
