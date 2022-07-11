package github.codeReaper2001.annotation;

import java.lang.annotation.*;

/**
 * RPC service annotation, marked on the service implementation class
 * RPC服务注解，标注在服务实现类上
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface RpcService {

    /**
     * Service version, default value is empty string
     */
    String version() default "";

    /**
     * Service group, default value is empty string
     */
    String group() default "";

}
