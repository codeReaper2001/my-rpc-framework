package github.codeReaper2001;

import github.codeReaper2001.config.ClientConfig;
import github.codeReaper2001.controller.IndexController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

// 当前版本用于测试，还未编写完成
@Slf4j
public class NettyClientMain {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ClientConfig.class);
        IndexController indexController = applicationContext.getBean(IndexController.class);
        indexController.index();
    }

}
