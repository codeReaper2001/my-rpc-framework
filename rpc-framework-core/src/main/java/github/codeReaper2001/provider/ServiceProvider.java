package github.codeReaper2001.provider;

import github.codeReaper2001.config.RpcServiceConfig;

public interface ServiceProvider {
    /**
     * @param rpcServiceConfig rpc service related attributes
     */
    void addService(RpcServiceConfig rpcServiceConfig);

    /**
     * @param rpcServiceName rpc service name
     * @return service object
     * 通过服务名获取服务
     */
    Object getService(String rpcServiceName);

    /**
     * @param rpcServiceConfig rpc service related attributes
     * 发布服务
     */
    void publishService(RpcServiceConfig rpcServiceConfig);
}
