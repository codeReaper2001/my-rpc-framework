package github.codeReaper2001.serialize;

import github.codeReaper2001.remoting.dto.RpcRequest;
import github.codeReaper2001.serialize.hessian.HessianSerializer;
import github.codeReaper2001.serialize.kyro.KryoSerializer;
import github.codeReaper2001.serialize.protostuff.ProtostuffSerializer;
import org.junit.Before;
import org.junit.Test;

public class TestSerializer {

    private RpcRequest rpcRequest;

    @Before
    public void buildRpcMessage() {
        Data jack = new Data("jack", 15);
        rpcRequest = RpcRequest.builder()
                .group("test")
                .version("version1")
                .requestId("1")
                .interfaceName("github.codeReaper2001.MyService")
                .methodName("hello")
                .parameters(new Object[]{jack})
                .paramTypes(new Class[]{Data.class})
                .build();
    }

    // 测试成功
    @Test
    public void testHessianSerializer() {
        Serializer serializer = new HessianSerializer();
        byte[] bytes = serializer.serialize(rpcRequest);
        RpcRequest result = serializer.deserialize(bytes, RpcRequest.class);
        System.out.println(result);
    }

    // 测试成功
    @Test
    public void testProtostuffSerializer() {
        Serializer serializer = new ProtostuffSerializer();
        byte[] bytes = serializer.serialize(rpcRequest);
        RpcRequest result = serializer.deserialize(bytes, RpcRequest.class);
        System.out.println(result);
    }

    // 测试失败
    @Test
    public void testKryoSerializer() {
        Serializer serializer = new KryoSerializer();
        byte[] bytes = serializer.serialize(rpcRequest);
        RpcRequest result = serializer.deserialize(bytes, RpcRequest.class);
        System.out.println(result);
    }
}
