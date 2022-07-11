package github.codeReaper2001.remoting.transport.netty.codec;

import github.codeReaper2001.compress.Compress;
import github.codeReaper2001.enums.CompressTypeEnum;
import github.codeReaper2001.enums.SerializationTypeEnum;
import github.codeReaper2001.extension.ExtensionLoader;
import github.codeReaper2001.remoting.constants.RpcConstants;
import github.codeReaper2001.remoting.dto.RpcMessage;
import github.codeReaper2001.remoting.dto.RpcRequest;
import github.codeReaper2001.remoting.dto.RpcResponse;
import github.codeReaper2001.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * <p>
 * custom protocol decoder
 * 将二进制流解码成RpcMessage
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
 * @see <a href="https://zhuanlan.zhihu.com/p/95621344">LengthFieldBasedFrameDecoder解码器</a>
 */
@Slf4j
public class RpcMessageDecoder extends LengthFieldBasedFrameDecoder {
    public RpcMessageDecoder() {
        // lengthFieldOffset: magic code is 4B, and version is 1B, and then full length. so value is 5
        // lengthFieldLength: full length is 4B. so value is 4
        // lengthAdjustment: full length include all data and read 9 bytes before, so the left length is (fullLength-9). so values is -9
        // initialBytesToStrip: we will check magic code and version manually, so do not strip any bytes. so values is 0
        this(RpcConstants.MAX_FRAME_LENGTH, 5, 4, -9, 0);
    }

    /**
     * @param maxFrameLength      Maximum frame length. It decide the maximum length of data that can be received.
     *                            If it exceeds, the data will be discarded.
     * @param lengthFieldOffset   Length field offset. The length field is the one that skips the specified length of byte.
     * @param lengthFieldLength   The number of bytes in the length field.
     * @param lengthAdjustment    The compensation value to add to the value of the length field
     * @param initialBytesToStrip Number of bytes skipped.
     *                            If you need to receive all of the header+body data, this value is 0
     *                            if you only want to receive the body data, then you need to skip the number of bytes consumed by the header.
     */
    public RpcMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                             int lengthAdjustment, int initialBytesToStrip) {
        // 使用父类的功能实现字节流按帧切割
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // 使用父类的功能实现字节流按帧切割（解决粘包半包问题）
        // 这里得到的decoded是完整的数据帧
        Object decoded = super.decode(ctx, in);
        if (decoded instanceof ByteBuf) {
            ByteBuf frame = (ByteBuf) decoded;
            if (frame.readableBytes() >= RpcConstants.TOTAL_LENGTH) {
                // 存在bodyBytes
                try {
                    return decodeFrame(frame);
                } catch (Exception e) {
                    log.error("Decode frame error!", e);
                    throw e;
                } finally {
                    // 手动释放直接内存
                    frame.release();
                }
            }

        }
        return decoded;
    }


    private Object decodeFrame(ByteBuf in) {
        // 检查魔数
        checkMagicNumber(in);
        // 版本号
        checkVersion(in);
        // 帧总长度 4B
        int fullLength = in.readInt();
        // 消息类型 1B
        byte messageType = in.readByte();
        // 序列化类型 1B
        byte codecType = in.readByte();
        // 压缩类型 1B
        byte compressType = in.readByte();
        // 请求Id 4B
        int requestId = in.readInt();
        RpcMessage rpcMessage = RpcMessage.builder()
                .messageType(messageType)
                .codec(codecType)
                .compress(compressType)
                .requestId(requestId).build();
        // 心跳包请求
        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            rpcMessage.setData(RpcConstants.PING);
            return rpcMessage;
        }
        if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            rpcMessage.setData(RpcConstants.PONG);
            return rpcMessage;
        }
        int bodyLength = fullLength - RpcConstants.HEAD_LENGTH;
        if (bodyLength > 0) {
            // 解析信息体内容
            byte[] bodyBuf = new byte[bodyLength];
            in.readBytes(bodyBuf);
            // decompress the bytes 解压缩内容
            String compressName = CompressTypeEnum.getName(compressType);
            Compress compress = ExtensionLoader.getExtensionLoader(Compress.class)
                    .getExtension(compressName);
            bodyBuf = compress.decompress(bodyBuf);
            // deserialize the object 反序列化
            String codecName = SerializationTypeEnum.getName(codecType);
            log.info("codec name: [{}] ", codecName);
            Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class)
                    .getExtension(codecName);
            if (messageType == RpcConstants.REQUEST_TYPE) {
                RpcRequest data = serializer.deserialize(bodyBuf, RpcRequest.class);
                rpcMessage.setData(data);
            }
            else {
                RpcResponse<?> data = serializer.deserialize(bodyBuf, RpcResponse.class);
                rpcMessage.setData(data);
            }
        }
        return rpcMessage;
    }

    // 检查帧首部的版本号是否对应
    private void checkVersion(ByteBuf in) {
        // read the version and compare
        byte version = in.readByte();
        if (version != RpcConstants.VERSION) {
            throw new RuntimeException("version isn't compatible" + version);
        }
    }

    // 检查帧首部的魔数是否对应
    private void checkMagicNumber(ByteBuf in) {
        // read the first 4 bit, which is the magic number, and compare
        int len = RpcConstants.MAGIC_NUMBER.length;
        byte[] tmp = new byte[len];
        in.readBytes(tmp);
        for (int i = 0; i < len; i++) {
            if (tmp[i] != RpcConstants.MAGIC_NUMBER[i]) {
                throw new IllegalArgumentException("Unknown magic code: " + Arrays.toString(tmp));
            }
        }
    }
}
