package github.codeReaper2001.loadbalance.loadbalancer;

import github.codeReaper2001.factory.SingletonFactory;
import github.codeReaper2001.loadbalance.AbstractLoadBalance;
import github.codeReaper2001.remoting.dto.RpcRequest;

import java.util.List;
import java.util.Random;

/**
 * Implementation of random load balancing strategy
 * 随机负载均衡策略的实现，即从列表中随机选取一个rpc服务地址
 */
public class RandomLoadBalance extends AbstractLoadBalance {

    private final Random random = SingletonFactory.getInstance(Random.class);

    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        return serviceAddresses.get(random.nextInt(serviceAddresses.size()));
    }
}
