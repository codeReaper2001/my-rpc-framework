package github.codeReaper2001.controller;

import github.codeReaper2001.Data;
import github.codeReaper2001.MyService;
import github.codeReaper2001.annotation.RpcReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class IndexController {

    @RpcReference(version = "version1", group = "test")
    private MyService myService;

    @GetMapping("/index")
    public String index() {
        return myService.hello(new Data("jack", 10));
    }

}
