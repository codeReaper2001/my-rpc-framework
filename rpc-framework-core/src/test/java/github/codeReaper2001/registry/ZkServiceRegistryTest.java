package github.codeReaper2001.registry;

import github.codeReaper2001.registry.zk.ZkServiceRegistryImpl;
import github.codeReaper2001.registry.zk.util.CuratorUtil;
import github.codeReaper2001.remoting.transport.netty.server.NettyRpcServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;

@Slf4j
public class ZkServiceRegistryTest {
    @Test
    public void testCreatePersistentNode() {
        CuratorFramework zkClient = CuratorUtil.getZkClient();
        CuratorUtil.createPersistentNode(zkClient, "/my-rpc/github.codeReaper2001.ServiceTest/172.30.207.112:8081");
    }

    @Test
    public void testGetChildrenNodes() {
        CuratorFramework zkClient = CuratorUtil.getZkClient();
        List<String> childrenNodes = CuratorUtil.getChildrenNodes(zkClient, "github.codeReaper2001.ServiceTest");
        System.out.println(childrenNodes);
    }

    // @Test
    // public void testClearRegistry() {
    //     CuratorFramework zkClient = CuratorUtil.getZkClient();
    //     CuratorUtil.clearRegistry(zkClient, new InetSocketAddress("172.30.207.112", 8081));
    // }

    @Test
    public void testZkServiceRegistryImpl() throws UnknownHostException {
        ZkServiceRegistryImpl zkServiceRegistry = new ZkServiceRegistryImpl();
        String host = InetAddress.getLocalHost().getHostAddress();
        zkServiceRegistry.registerService("github.codeReaper2001.ServiceTest", new InetSocketAddress(host, NettyRpcServer.PORT));
    }
}
