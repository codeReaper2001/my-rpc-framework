import config.TestClientConfig;
import controller.TestController;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/*
* 用于测试QPS
* */
public class ClientTestMain {
    public static void main(String[] args) {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "Error");

        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(TestClientConfig.class);
        TestController controller = applicationContext.getBean(TestController.class);
        controller.test();
    }
}
