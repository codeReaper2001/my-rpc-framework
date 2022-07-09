package github.codeReaper2001.handler;

import github.codeReaper2001.exception.RpcException;
import github.codeReaper2001.factory.SingletonFactory;
import github.codeReaper2001.provider.ServiceProvider;
import github.codeReaper2001.provider.impl.ZkServiceProviderImpl;
import github.codeReaper2001.remoting.dto.RpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class RpcRequestHandler {
    private final ServiceProvider serviceProvider;

    public RpcRequestHandler() {
        // 从单例工厂中获取ServiceProvider实例
        serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }

    /**
     * Processing rpcRequest: call the corresponding method, and then return the result
     * 处理rpcRequest：调用对应的方法，然后返回方法调用结果
     */
    public Object handle(RpcRequest rpcRequest) {
        Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
        return invokeTargetMethod(rpcRequest, service);
    }

    /**
     * get method execution results
     * ① 根据RpcRequest对象中的方法名和参数类型找到对应的方法
     * ② 调用service实例的对应方法，将RpcRequest中的参数填入，返回方法调用结果
     * @param rpcRequest client request
     * @param service    service object
     * @return the result of the target method execution
     */
    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            result = method.invoke(service, rpcRequest.getParameters());
            log.info("service:[{}] successful invoke method:[{}]", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RpcException(e.getMessage(), e);
        }
        return result;
    }
}
