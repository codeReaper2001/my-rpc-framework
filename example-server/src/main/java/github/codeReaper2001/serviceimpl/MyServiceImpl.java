package github.codeReaper2001.serviceimpl;

import github.codeReaper2001.Data;
import github.codeReaper2001.MyService;

public class MyServiceImpl implements MyService {
    @Override
    public String hello(Data data) {
        return "服务器端MyService服务处理后的结果：" + data.toString();
    }
}
