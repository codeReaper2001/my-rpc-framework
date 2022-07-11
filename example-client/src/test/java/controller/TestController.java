package controller;

import github.codeReaper2001.Data;
import github.codeReaper2001.MyService;
import github.codeReaper2001.annotation.RpcReference;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Component
public class TestController {

    @RpcReference(version = "version1", group = "test")
    private MyService myService;

    public void test() {
        int threadNum = 100000;
        int n = 100;
        CountDownLatch countDownLatch = new CountDownLatch(threadNum);

        long startTime = System.currentTimeMillis();
        for (int t = 0; t < threadNum; t++) {
            new Thread(()-> {
                for (int i = 0; i < n; i++) {
                    String result = myService.hello(new Data("jack" + i, 10 + i));
                }
                countDownLatch.countDown();
            }).start();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        float during = (float) (endTime - startTime)/ 1000;
        System.out.printf("发送%d个请求，时间：%f s，qps：%f\n", threadNum * n, during, threadNum * n/during);
    }

}
