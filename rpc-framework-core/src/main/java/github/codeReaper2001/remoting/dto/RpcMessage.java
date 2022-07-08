package github.codeReaper2001.remoting.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcMessage {
    /**
     * rpc message type
     * 标注data的类型
     * 1:RpcRequest 2.RpcResponse 3.心跳包Request 4.心跳包Response
     */
    private byte messageType;

    /**
     * serialization type
     * 序列化算法
     */
    private byte codec;

    /**
     * compress type
     * 压缩算法
     */
    private byte compress;

    /**
     * request id
     */
    private int requestId;

    /**
     * request data
     * 真正的携带的数据，可能为RpcRequest或RpcResponse
     */
    private Object data;
}
