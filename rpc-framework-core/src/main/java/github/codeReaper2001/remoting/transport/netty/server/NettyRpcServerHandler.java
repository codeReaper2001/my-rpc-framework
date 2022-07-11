package github.codeReaper2001.remoting.transport.netty.server;

import github.codeReaper2001.enums.CompressTypeEnum;
import github.codeReaper2001.enums.RpcResponseCodeEnum;
import github.codeReaper2001.enums.SerializationTypeEnum;
import github.codeReaper2001.factory.SingletonFactory;
import github.codeReaper2001.handler.RpcRequestHandler;
import github.codeReaper2001.remoting.constants.RpcConstants;
import github.codeReaper2001.remoting.dto.RpcMessage;
import github.codeReaper2001.remoting.dto.RpcRequest;
import github.codeReaper2001.remoting.dto.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Customize the ChannelHandler of the server to process the data sent by the client.
 * <p>
 * 如果继承自 SimpleChannelInboundHandler 的话就不要考虑 ByteBuf 的释放 ，{@link SimpleChannelInboundHandler} 内部的
 * channelRead 方法会替你释放 ByteBuf ，避免可能导致的内存泄露问题。详见《Netty进阶之路 跟着案例学 Netty》
 */
@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {

    private final RpcRequestHandler rpcRequestHandler;

    public NettyRpcServerHandler() {
        // 从工厂中获取单例
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof RpcMessage) {
                log.info("server receive msg: [{}] ", msg);
                RpcMessage inMessage = (RpcMessage)msg;
                byte messageType = inMessage.getMessageType();
                RpcMessage outMessage = new RpcMessage();
                outMessage.setCodec(SerializationTypeEnum.HESSIAN.getCode());
                outMessage.setCompress(CompressTypeEnum.GZIP.getCode());

                // 判断是否是心跳包
                if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                    outMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                    outMessage.setData(RpcConstants.PONG);
                } else {
                    RpcRequest rpcRequest = (RpcRequest) inMessage.getData();
                    // Execute the target method (the method the client needs to execute) and return the method result
                    // 执行目标方法（客户端需要执行的方法）并返回方法结果
                    Object result = rpcRequestHandler.handle(rpcRequest);
                    log.info(String.format("server get result: %s", result.toString()));
                    outMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                    // 查看通道是否可用（连接是否正常）
                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                        RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                        outMessage.setData(rpcResponse);
                    } else {
                        RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                        outMessage.setData(rpcResponse);
                        log.error("not writable now, message dropped");
                    }
                }
                // 发送RpcMessage回客户端
                ctx.writeAndFlush(outMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } finally {
            // Ensure that ByteBuf is released, otherwise there may be memory leaks
            // 确保释放ByteBuf，否则可能会出现内存泄漏
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 外面设置的Idle事件，这里是30s无输入断开连接
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                log.info("idle check happen, so close the connection");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("server catch exception");
        cause.printStackTrace();
        ctx.close();
    }
}
