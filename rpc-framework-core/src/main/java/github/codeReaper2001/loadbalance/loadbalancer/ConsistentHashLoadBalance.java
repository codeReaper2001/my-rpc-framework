package github.codeReaper2001.loadbalance.loadbalancer;

import github.codeReaper2001.loadbalance.AbstractLoadBalance;
import github.codeReaper2001.remoting.dto.RpcRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * refer to dubbo consistent hash load balance: https://github.com/apache/dubbo/blob/2d9583adf26a2d8bd6fb646243a9fe80a77e65d5/dubbo-cluster/src/main/java/org/apache/dubbo/rpc/cluster/loadbalance/ConsistentHashLoadBalance.java
 * 一致性哈希策略
 * 一致性哈希原理：https://xiaolincoding.com/os/8_network_system/hash.html
 */
public class ConsistentHashLoadBalance extends AbstractLoadBalance {

    /*
    * key: rpcServiceName
    * value: ConsistentHashSelector对象
    * */
    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        // 获取serviceAddresses对象的哈希码
        int identityHashCode = System.identityHashCode(serviceAddresses);
        String rpcServiceName = rpcRequest.getRpcServiceName();
        ConsistentHashSelector selector = selectors.get(rpcServiceName);
        // check for updates
        if (selector == null || selector.identityHashCode != identityHashCode) {
            // 创建ConsistentHashSelector实例，160表示一个address分配160个虚拟节点
            selectors.put(rpcServiceName, new ConsistentHashSelector(serviceAddresses, 160, identityHashCode));
            selector = selectors.get(rpcServiceName);
        }
        // rpcServiceKey = 服务名 + 填入的参数数组字符串，保证多次调用到不同的机器上的服务
        return selector.select(rpcServiceName + Arrays.stream(rpcRequest.getParameters()));
    }

    static class ConsistentHashSelector {
        // 虚拟节点
        private final TreeMap<Long, String> virtualNodes;
        // 一致性哈希码，对象标识
        private final int identityHashCode;

        ConsistentHashSelector(List<String> addresses, int replicaNumber, int identityHashCode) {
            this.virtualNodes = new TreeMap<>();
            this.identityHashCode = identityHashCode;

            for (String address : addresses) {
                // 一个address分配replicaNumber个虚拟节点
                for (int i = 0; i < replicaNumber / 4; i++) {
                    // md5("{ip}:{port}{i}") -> byte数组
                    byte[] digest = md5(address + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        virtualNodes.put(m, address);
                    }
                }
            }
        }

        // md5摘要算法转为byte数组
        static byte[] md5(String key) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            return md.digest();
        }

        // 参考github上的代码
        private long hash(byte[] digest, int number) {
            return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                    | (digest[number * 4] & 0xFF))
                    & 0xFFFFFFFFL;
        }

        public String select(String rpcServiceKey) {
            byte[] digest = md5(rpcServiceKey);
            return selectForKey(hash(digest, 0));
        }

        // 输入哈希码，返回选择的主机({host}:{port})
        public String selectForKey(long hashCode) {
            Map.Entry<Long, String> entry = virtualNodes.tailMap(hashCode, true).firstEntry();

            if (entry == null) {
                entry = virtualNodes.firstEntry();
            }

            return entry.getValue();
        }
    }

}
