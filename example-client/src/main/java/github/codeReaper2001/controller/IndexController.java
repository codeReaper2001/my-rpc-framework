package github.codeReaper2001.controller;

import github.codeReaper2001.Data;
import github.codeReaper2001.MyService;
import github.codeReaper2001.annotation.RpcReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IndexController {

    @RpcReference(version = "version1", group = "test")
    private MyService myService;

    public void index() {
        for (int i = 0; i < 5; i++) {
            String result = myService.hello(new Data("jack" + i, 10 + i));
            log.info("第{}次，调用结果：{}", i, result);
        }
    }

}
