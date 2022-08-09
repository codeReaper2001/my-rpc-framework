package github.codeReaper2001.provider.impl;

import github.codeReaper2001.config.RpcServiceConfig;
import github.codeReaper2001.enums.RpcErrorMessageEnum;
import github.codeReaper2001.exception.RpcException;
import github.codeReaper2001.extension.ExtensionLoader;
import github.codeReaper2001.provider.ServiceProvider;
import github.codeReaper2001.registry.ServiceRegistry;
import github.codeReaper2001.remoting.transport.netty.server.NettyRpcServer;
import github.codeReaper2001.utils.IPUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ZkServiceProviderImpl implements ServiceProvider {

    /**
     * key: rpc service name(interface name + version + group)
     * value: service object
     */
    private final Map<String, Object> serviceMap;
    private final ServiceRegistry serviceRegistry;

    public ZkServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        // 默认的服务注册中心为zookeeper
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class)
                .getExtension("zk");
    }


    // 添加服务到自己的map上
    @Override
    public void addService(RpcServiceConfig rpcServiceConfig) {
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        if (serviceMap.containsKey(rpcServiceName)) {
            return;
        }
        serviceMap.putIfAbsent(rpcServiceName, rpcServiceConfig.getService());
        log.info("Add service: {} and interfaces:{}", rpcServiceName, rpcServiceConfig.getService().getClass().getInterfaces());
    }

    @Override
    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {
        try {
            String host = IPUtil.getLocalHostIPStr();
            // 先将服务添加到map容器上
            this.addService(rpcServiceConfig);
            // 然后发布到注册中心(服务名，ip地址，即告诉其他rpc客户端自己这里有该服务)
            serviceRegistry.registerService(rpcServiceConfig.getRpcServiceName(), new InetSocketAddress(host, NettyRpcServer.PORT));
        } catch (UnknownHostException e) {
            log.error("occur exception when getHostAddress", e);
        }
    }
}
