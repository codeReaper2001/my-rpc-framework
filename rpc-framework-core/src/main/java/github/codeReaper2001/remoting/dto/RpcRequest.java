package github.codeReaper2001.remoting.dto;

import lombok.*;

import java.io.Serializable;

/*
* Rpc请求对象
* */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1905122041950251207L;
    private String requestId;
    // 接口名
    private String interfaceName;
    // 方法名
    private String methodName;
    // 方法参数
    private Object[] parameters;
    // 各参数类型（方法重载时可以找到对应方法）
    private Class<?>[] paramTypes;
    // 当前Rpc请求版本，方便升级
    private String version;

    private String group;

    public String getRpcServiceName() {
        return this.interfaceName + this.group + this.version;
    }
}
