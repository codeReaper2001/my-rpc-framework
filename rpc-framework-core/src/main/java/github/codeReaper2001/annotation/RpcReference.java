package github.codeReaper2001.annotation;

import java.lang.annotation.*;

/**
 * RPC reference annotation, autowire the service implementation class
 * RPC 引用注解，自动装配rpc客户端服务代理对象
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
public @interface RpcReference {

    /**
     * Service version, default value is empty string
     */
    String version() default "";

    /**
     * Service group, default value is empty string
     */
    String group() default "";

}
