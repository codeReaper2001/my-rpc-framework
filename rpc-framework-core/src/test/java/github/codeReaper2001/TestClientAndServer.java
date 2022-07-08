package github.codeReaper2001;

import github.codeReaper2001.enums.CompressTypeEnum;
import github.codeReaper2001.enums.SerializationTypeEnum;
import github.codeReaper2001.remoting.constants.RpcConstants;
import github.codeReaper2001.remoting.dto.RpcMessage;
import github.codeReaper2001.remoting.dto.RpcRequest;
import github.codeReaper2001.remoting.transport.netty.codec.RpcMessageDecoder;
import github.codeReaper2001.remoting.transport.netty.codec.RpcMessageEncoder;
import github.codeReaper2001.remoting.transport.netty.server.NettyRpcServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class TestClientAndServer {
    @Test
    public void startServer() {
        NettyRpcServer nettyRpcServer = new NettyRpcServer();
        nettyRpcServer.start();
    }

    @Test
    public void startClient() {
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(worker);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                    ch.pipeline().addLast(new RpcMessageEncoder());
                    ch.pipeline().addLast(new RpcMessageDecoder());
                    ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                        // 会在连接 channel 建立成功后，会触发 active 事件
                        @Override
                        public void channelActive(ChannelHandlerContext ctx) {
                            log.info("tcp连接成功");
                            RpcMessage rpcMessage = RpcMessage.builder()
                                    .messageType(RpcConstants.REQUEST_TYPE)
                                    .compress(CompressTypeEnum.GZIP.getCode())
                                    .codec(SerializationTypeEnum.KYRO.getCode())
                                    .data(new RpcRequest())
                                    .requestId(1)
                                    .build();
                            ctx.writeAndFlush(rpcMessage);
                        }

                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                            log.info("服务器返回信息：{}", msg);
                        }
                    });
                }
            });
            ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 8081).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("client error", e);
        } finally {
            worker.shutdownGracefully();
        }
    }

    @Test
    public void testEncoder() {
        RpcMessageEncoder rpcMessageEncoder = new RpcMessageEncoder();
        RpcMessageDecoder rpcMessageDecoder = new RpcMessageDecoder();
        EmbeddedChannel channel = new EmbeddedChannel(
                new LoggingHandler(LogLevel.INFO),
                rpcMessageEncoder,
                rpcMessageDecoder);
        RpcMessage rpcMessage = RpcMessage.builder()
                .messageType(RpcConstants.REQUEST_TYPE)
                .compress(CompressTypeEnum.GZIP.getCode())
                .codec(SerializationTypeEnum.HESSIAN.getCode())
                .data(new RpcRequest())
                .requestId(1)
                .build();
        channel.writeOutbound(rpcMessage);
        // channel.writeOutbound(ByteBufAllocator.DEFAULT.buffer().writeBytes(RpcConstants.MAGIC_NUMBER));
    }

}
