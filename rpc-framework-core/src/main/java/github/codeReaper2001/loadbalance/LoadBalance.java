package github.codeReaper2001.loadbalance;

import github.codeReaper2001.extension.SPI;
import github.codeReaper2001.remoting.dto.RpcRequest;

import java.util.List;

/**
 * Interface to the load balancing policy
 * 负载均衡策略接口
 */
@SPI
public interface LoadBalance {
    /**
     * Choose one from the list of existing service addresses list
     * 从现有服务地址列表中选择一个
     *
     * @param serviceUrlList 服务地址列表
     * @param rpcRequest rpc请求
     * @return 目标服务地址
     */
    String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest);
}
