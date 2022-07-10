package github.codeReaper2001.remoting.transport.netty.client;

import github.codeReaper2001.enums.CompressTypeEnum;
import github.codeReaper2001.enums.SerializationTypeEnum;
import github.codeReaper2001.extension.ExtensionLoader;
import github.codeReaper2001.factory.SingletonFactory;
import github.codeReaper2001.registry.ServiceDiscovery;
import github.codeReaper2001.remoting.constants.RpcConstants;
import github.codeReaper2001.remoting.dto.RpcMessage;
import github.codeReaper2001.remoting.dto.RpcRequest;
import github.codeReaper2001.remoting.dto.RpcResponse;
import github.codeReaper2001.remoting.transport.RpcRequestTransport;
import github.codeReaper2001.remoting.transport.netty.codec.RpcMessageDecoder;
import github.codeReaper2001.remoting.transport.netty.codec.RpcMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class NettyRpcClient implements RpcRequestTransport {
    // 服务发现器
    private final ServiceDiscovery serviceDiscovery;
    // 未处理的rpc请求
    private final UnprocessedRequests unprocessedRequests;
    // 连接Channel容器
    private final ChannelProvider channelProvider;

    private final Bootstrap bootstrap;
    // 读写线程池
    private final EventLoopGroup eventLoopGroup;

    public NettyRpcClient() {
        // 读写线程池
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();

        // 创建收到RpcMessage后的处理器，并添加
        NettyRpcClientHandler nettyRpcClientHandler = new NettyRpcClientHandler(this);
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                // 设置连接超时时间
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new RpcMessageEncoder());
                        pipeline.addLast(new RpcMessageDecoder());
                        pipeline.addLast(nettyRpcClientHandler);
                    }
                });
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class)
                .getExtension("zk");
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
    }

    /**
     * connect server and get the channel ,so that you can send rpc message to server
     *
     * @param inetSocketAddress server address
     * @return the channel
     * 根据url建立连接并得到 Channel 实例
     */
    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            }
            else {
                throw new IllegalStateException();
            }
        });
        return completableFuture.get();
    }

    @Override
    public CompletableFuture<RpcResponse<Object>> sendRpcRequest(RpcRequest rpcRequest) {
        // build return value
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        // 获取rpc服务器地址
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        // 连接到服务器
        Channel channel = getChannel(inetSocketAddress);
        if (channel.isActive()) {
            // 将未处理请求添加到集合(未处理请求id -> future)
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            // 构造发送的RpcMessage，由于编码器会填充requestId字段，故这里不需要填
            RpcMessage rpcMessage = RpcMessage.builder()
                    .codec(SerializationTypeEnum.PROTOSTUFF.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .messageType(RpcConstants.REQUEST_TYPE)
                    .data(rpcRequest).build();
            // 发送RpcMessage对象
            channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener)future -> {
                if (future.isSuccess()) {
                    log.info("client send message: [{}]", rpcMessage);
                } else {
                    future.channel().close();
                    resultFuture.completeExceptionally(future.cause());
                    log.error("Send failed:", future.cause());
                }
            });
        } else {
            throw new IllegalStateException();
        }
        return resultFuture;
    }

    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        if (channel == null) {
            // 如果不存在该连接，则进行连接，并在连接后将Channel实例放入集合
            channel = doConnect(inetSocketAddress);
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }

    public void close() {
        eventLoopGroup.shutdownGracefully();
    }
}
