package github.codeReaper2001.remoting.transport.netty.codec;

import github.codeReaper2001.compress.Compress;
import github.codeReaper2001.enums.CompressTypeEnum;
import github.codeReaper2001.enums.SerializationTypeEnum;
import github.codeReaper2001.extension.ExtensionLoader;
import github.codeReaper2001.remoting.constants.RpcConstants;
import github.codeReaper2001.remoting.dto.RpcMessage;
import github.codeReaper2001.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * custom protocol decoder
 * 将RpcMessage编码成二进制流
 * <p>
 * 协议：消息头部16B，消息体 (full length - 16)B
 * <pre>
 *   0    1    2    3    4        5     6    7    8    9          10    11      12    13   14  15   16
 *   +----+----+----+----+--------+----+----+----+----+-----------+------+----- --+----+----+----+----+
 *   |   magic   code    |version |    full length    |messageType| codec|compress|    RequestId      |
 *   +-------------------+--------+-------------------+-----------+------+--------+-------------------+
 *   |                                                                                                |
 *   |                                         body                                                   |
 *   |                                                                                                |
 *   |                                        ... ...                                                 |
 *   +------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B codec（序列化类型）     1B compress（压缩类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 * </pre>
 */

@Slf4j
public class RpcMessageEncoder extends MessageToByteEncoder<RpcMessage> {

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage rpcMessage, ByteBuf out) throws Exception {
        try {
            // 魔数 4B
            out.writeBytes(RpcConstants.MAGIC_NUMBER);
            // 版本 1B
            out.writeByte(RpcConstants.VERSION);
            // 消息长度 4B，这里暂时先空出来到时候得到长度再填进去
            // leave a place to write the value of full length
            out.writerIndex(out.writerIndex() + 4);
            // 消息类型：1.RpcRequest 2.RpcResponse (1B)
            byte messageType = rpcMessage.getMessageType();
            out.writeByte(messageType);
            // 序列化类型 1B
            out.writeByte(rpcMessage.getCodec());
            // 压缩类型 1B，这里只有一种压缩类型GZIP
            out.writeByte(CompressTypeEnum.GZIP.getCode());
            // 请求的Id 4B
            out.writeInt(ATOMIC_INTEGER.getAndIncrement());

            // build full length
            byte[] bodyBytes = null;
            int fullLength = RpcConstants.HEAD_LENGTH;

            // if messageType is not heartbeat message，fullLength = head length + body length
            // 如果是心跳包则body内容为空
            if (messageType != RpcConstants.HEARTBEAT_REQUEST_TYPE
                    && messageType != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                // serialize the object 序列化RpcMessage实例
                // 根据发送信报上的序列化方式编号得到序列化方式名
                String codecName = SerializationTypeEnum.getName(rpcMessage.getCodec());
                log.info("codec name: [{}] ", codecName);
                Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class)
                        .getExtension(codecName);
                bodyBytes = serializer.serialize(rpcMessage.getData());
                // compress the bytes 压缩
                String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());
                Compress compress = ExtensionLoader.getExtensionLoader(Compress.class)
                        .getExtension(compressName);
                bodyBytes = compress.compress(bodyBytes);
                fullLength += bodyBytes.length;
            }

            if (bodyBytes != null) {
                out.writeBytes(bodyBytes);
            }

            int writerIndex = out.writerIndex();
            // 将读指针移动到fullLength字段
            out.writerIndex(writerIndex - fullLength + RpcConstants.MAGIC_NUMBER.length + 1);
            // 消息长度 4B
            out.writeInt(fullLength);
            // 恢复读指针
            out.writerIndex(writerIndex);

            log.info("{}", out);
        } catch (Exception e) {
            log.error("Encode request error!", e);
        }
    }
}
