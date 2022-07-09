package github.codeReaper2001.enums;

import lombok.Getter;

@Getter
public enum  RpcConfigEnum {

    RPC_CONFIG_PATH("rpc.properties"),
    ZK_ADDRESS("rpc.zookeeper.address");

    private final String propertyValue;

    RpcConfigEnum(String propertyValue) {
        this.propertyValue = propertyValue;
    }
}
