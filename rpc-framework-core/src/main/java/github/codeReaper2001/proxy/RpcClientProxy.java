package github.codeReaper2001.proxy;

import github.codeReaper2001.config.RpcServiceConfig;
import github.codeReaper2001.enums.RpcErrorMessageEnum;
import github.codeReaper2001.enums.RpcResponseCodeEnum;
import github.codeReaper2001.exception.RpcException;
import github.codeReaper2001.remoting.dto.RpcRequest;
import github.codeReaper2001.remoting.dto.RpcResponse;
import github.codeReaper2001.remoting.transport.RpcRequestTransport;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class RpcClientProxy implements InvocationHandler {

    private static final String INTERFACE_NAME = "interfaceName";
    // 用来发送Rpc请求
    private final RpcRequestTransport rpcRequestTransport;
    // 用于构造RpcMessage(对应上group和version)
    private final RpcServiceConfig rpcServiceConfig;

    public RpcClientProxy(RpcRequestTransport rpcRequestTransport, RpcServiceConfig rpcServiceConfig) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.rpcServiceConfig = rpcServiceConfig;
    }

    public RpcClientProxy(RpcRequestTransport rpcRequestTransport) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.rpcServiceConfig = new RpcServiceConfig();
    }

    /**
     * get the proxy object
     * @param interfaceClazz 接口class对象
     * 获取代理对象（暂时使用jdk动态代理）
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> interfaceClazz) {
        return (T) Proxy.newProxyInstance(interfaceClazz.getClassLoader(),
                new Class[]{interfaceClazz},
                this);
    }

    /*
    * 代理对象所有方法的调用最终都会转为调用invoke方法
    * 因此在invoke方法中实现发送RpcMessage完成远程过程调用
    * */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("invoked method: [{}]", method.getName());
        // 构造rpc请求对象
        RpcRequest rpcRequest = RpcRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .paramTypes(method.getParameterTypes())
                .parameters(args)
                .requestId(UUID.randomUUID().toString())
                .group(rpcServiceConfig.getGroup())
                .version(rpcServiceConfig.getVersion()).build();
        // 发送请求
        CompletableFuture<RpcResponse<Object>> completableFuture = rpcRequestTransport.sendRpcRequest(rpcRequest);
        // 阻塞等待响应
        RpcResponse<Object> rpcResponse = completableFuture.get();
        // 检查相应数据
        this.check(rpcResponse, rpcRequest);
        // 最后将远程过程调用结果返回
        return rpcResponse.getData();
    }

    // 检查返回的数据
    private void check(RpcResponse<Object> rpcResponse, RpcRequest rpcRequest) {
        if (rpcResponse == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
        // 检查返回数据包的requestId是否对应
        if (!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())) {
            throw new RpcException(RpcErrorMessageEnum.REQUEST_NOT_MATCH_RESPONSE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
        // 检查是否成功完成调用
        if (rpcResponse.getCode() == null || !rpcResponse.getCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
    }
}
