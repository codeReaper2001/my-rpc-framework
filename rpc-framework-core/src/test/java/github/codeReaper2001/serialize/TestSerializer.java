package github.codeReaper2001.serialize;

import github.codeReaper2001.enums.CompressTypeEnum;
import github.codeReaper2001.enums.SerializationTypeEnum;
import github.codeReaper2001.remoting.constants.RpcConstants;
import github.codeReaper2001.remoting.dto.RpcMessage;
import github.codeReaper2001.remoting.dto.RpcRequest;
import github.codeReaper2001.serialize.hessian.HessianSerializer;
import github.codeReaper2001.serialize.kyro.KryoSerializer;
import github.codeReaper2001.serialize.protostuff.ProtostuffSerializer;
import org.junit.Before;
import org.junit.Test;

public class TestSerializer {

    private RpcMessage rpcMessage;
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

        rpcMessage = RpcMessage.builder()
                .messageType(RpcConstants.REQUEST_TYPE)
                .compress(CompressTypeEnum.GZIP.getCode())
                .codec(SerializationTypeEnum.HESSIAN.getCode())
                .data(rpcRequest)
                .build();
    }

    // 测试失败
    @Test
    public void testHessianSerializer() {
        Serializer serializer = new HessianSerializer();
        byte[] bytes = serializer.serialize(rpcMessage);
        RpcMessage result = serializer.deserialize(bytes, RpcMessage.class);
        System.out.println(result);
    }

    // 测试成功
    @Test
    public void testProtostuffSerializer() {
        Serializer serializer = new ProtostuffSerializer();
        byte[] bytes = serializer.serialize(rpcMessage);
        RpcMessage result = serializer.deserialize(bytes, RpcMessage.class);
        System.out.println(result);
    }

    // 测试失败
    @Test
    public void testKryoSerializer() {
        Serializer serializer = new KryoSerializer();
        byte[] bytes = serializer.serialize(rpcMessage);
        RpcMessage result = serializer.deserialize(bytes, RpcMessage.class);
        System.out.println(result);
    }
}
