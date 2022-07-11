package github.codeReaper2001.serviceimpl;

import github.codeReaper2001.Data;
import github.codeReaper2001.MyService;
import github.codeReaper2001.annotation.RpcService;

@RpcService(version = "version2", group = "test")
public class MyServiceImpl2 implements MyService {
    @Override
    public String hello(Data data) {
        return "服务器端MyService服务处理后的结果(MyServiceImpl2)：" + data.toString();
    }
}
