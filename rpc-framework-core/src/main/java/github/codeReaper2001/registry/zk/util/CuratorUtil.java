package github.codeReaper2001.registry.zk.util;

import github.codeReaper2001.enums.RpcConfigEnum;
import github.codeReaper2001.utils.PropertiesFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CuratorUtil {
    private static final int BASE_SLEEP_TIME = 1000;
    // 最大重试次数
    private static final int MAX_RETRIES = 3;
    // 服务注册根目录
    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";
    // 服务地址map：key -> rpc服务名，value -> 服务器地址列表
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    // zk节点路径集合
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();

    private static CuratorFramework zkClient;
    // 默认zookeeper地址
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";

    private CuratorUtil() {
    }

    /**
     * Create persistent nodes. Unlike temporary nodes, persistent nodes are not removed when the client disconnects
     * 创建持久节点。与临时节点不同，持久节点在客户端断开连接时不会被移除
     * @param path node path
     */
    public static void createPersistentNode(CuratorFramework zkClient, String path) {
        try {
            // 先判断节点是否已存在
            // 使用REGISTERED_PATH_SET是为了更快地判断
            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                // 节点已存在
                log.info("The node already exists. The node is:[{}]", path);
            } else {
                // 节点不存在，创建节点
                //eg: /my-rpc/github.codeReaper2001.HelloService/127.0.0.1:9999
                zkClient.create().creatingParentContainersIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("The node was created successfully. The node is:[{}]", path);
            }
            REGISTERED_PATH_SET.add(path);
        } catch (Exception e) {
            log.error("create persistent node for path [{}] fail", path);
        }
    }

    /**
     * Gets the children under a node
     *
     * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version1
     * @return All child nodes under the specified node
     * 即获取某rpc服务的所有服务器ip地址列表
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
        // 先从缓存中找
        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }
        // 缓存中没有，则查询zk
        List<String> result = null;
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        try {
            result = zkClient.getChildren().forPath(servicePath);
            // 将结果放到缓存中
            SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
            // 监听节点变化
            registerWatcher(rpcServiceName, zkClient);
        } catch (Exception e) {
            log.error("get children nodes for path [{}] fail", servicePath);
        }
        return result;
    }

    /**
     * Empty the registry of data
     * 取消注册，即将某ip地址主机的所有服务下线
     */
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        REGISTERED_PATH_SET.stream().parallel().forEach(p-> {
            // 遍历每个已注册的路径，如果发现路径结尾为输入的ip地址，则删除这个节点
            // eg：输入Address：192.168.0.0.1:9999
            // 存在节点 /my-rpc/github.codeReaper2001.Service1/192.168.0.0.1:9999，则该节点被删除
            try {
                if (p.endsWith(inetSocketAddress.toString())) {
                    zkClient.delete().forPath(p);
                }
            } catch (Exception e) {
                log.error("clear registry for path [{}] fail", p);
            }
        });
        log.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET.toString());
    }


    public static CuratorFramework getZkClient() {
        // if zkClient has been started, return directly
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }
        // check if user has set zk address
        // 先查看是否存在对应的配置和文件和对应配置项，如果有则覆盖默认zk address值
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        String zookeeperAddress = properties != null && properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null
                ? properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue())
                : DEFAULT_ZOOKEEPER_ADDRESS;
        // Retry strategy. Retry 3 times, and will increase the sleep time between retries.
        // 重试策略。重试 3 次，会增加重试之间的休眠时间
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        // 构造CuratorFramework对象zkClient
        zkClient = CuratorFrameworkFactory.builder()
                .connectString(zookeeperAddress)
                .retryPolicy(retryPolicy)
                .build();
        // 连接到zookeeper服务器
        zkClient.start();
        try {
            // wait 30s until connect to the zookeeper
            // 等待 30 秒，直到连接到 zookeeper
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Time out waiting to connect to ZK!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return zkClient;
    }

    /**
     * Registers to listen for changes to the specified node
     * 注册监听指定节点的变化，当变化时更新SERVICE_ADDRESS_MAP中key为rpcServiceName键值对的value
     *
     * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version
     */
    private static void registerWatcher(String rpcServiceName, CuratorFramework zkClient) throws Exception {
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);
        PathChildrenCacheListener pathChildrenCacheListener = (curatorFramework, pathChildrenCacheEvent) -> {
            // 每次/my-rpc/{rpcServiceName}路径节点发生变化，就会更新SERVICE_ADDRESS_MAP
            // 例如某主机服务下线，就不会再访问对应服务
            List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
        };
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        pathChildrenCache.start();
    }

}
