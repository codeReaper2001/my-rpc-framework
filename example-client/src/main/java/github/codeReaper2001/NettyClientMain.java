package github.codeReaper2001;

import github.codeReaper2001.config.ClientConfig;
import github.codeReaper2001.controller.IndexController;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class NettyClientMain {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ClientConfig.class);
        IndexController indexController = applicationContext.getBean(IndexController.class);
        indexController.index();
    }

}
