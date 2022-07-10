package github.codeReaper2001.remoting.transport;

import github.codeReaper2001.remoting.dto.RpcRequest;
import github.codeReaper2001.remoting.dto.RpcResponse;

import java.util.concurrent.CompletableFuture;

public interface RpcRequestTransport {
    /**
     * send rpc request to server and get result
     *
     * @param rpcRequest message body
     * @return data from server
     * 发送Rpc请求并返回结果
     */
    CompletableFuture<RpcResponse<Object>> sendRpcRequest(RpcRequest rpcRequest);
}
