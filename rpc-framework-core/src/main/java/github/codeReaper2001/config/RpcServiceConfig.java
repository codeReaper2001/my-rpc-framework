package github.codeReaper2001.config;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class RpcServiceConfig {
    /**
     * service version
     */
    private String version = "";
    /**
     * when the interface has multiple implementation classes, distinguish by group
     * 当接口有多个实现类时，按组区分
     */
    private String group = "";

    /**
     * target service
     * 服务对象
     */
    private Object service;

    public String getRpcServiceName() {
        // Rpc服务名 = 服务接口名 + 组名 + 版本号
        return this.getServiceName() + this.getGroup() + this.getVersion();
    }

    public String getServiceName() {
        // 即服务类实现的服务接口名
        return this.service.getClass().getInterfaces()[0].getCanonicalName();
    }
}
