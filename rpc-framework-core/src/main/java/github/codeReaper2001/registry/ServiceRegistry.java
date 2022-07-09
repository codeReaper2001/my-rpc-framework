package github.codeReaper2001.registry;

import github.codeReaper2001.extension.SPI;

import java.net.InetSocketAddress;

/**
 * service registration
 * 服务注册接口
 */
@SPI
public interface ServiceRegistry {
    /**
     * register service
     * 注册服务
     * @param rpcServiceName    rpc service name
     * @param inetSocketAddress service address
     */
    void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress);
}
