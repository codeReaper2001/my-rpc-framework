package github.codeReaper2001.registry;

import github.codeReaper2001.extension.SPI;
import github.codeReaper2001.remoting.dto.RpcRequest;

import java.net.InetSocketAddress;

/*
* service discovery
* 服务发现接口，rpc客户端使用，用来寻找提供对应服务的主机
* */
@SPI
public interface ServiceDiscovery {
    /**
     * lookup service by rpcServiceName
     *
     * @param rpcRequest rpc service pojo
     * @return service address
     */
    InetSocketAddress lookupService(RpcRequest rpcRequest);
}
