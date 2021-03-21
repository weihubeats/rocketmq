package org.apache.rocketmq.broker;

import org.apache.rocketmq.common.namesrv.RegisterBrokerResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CountDownLatchTest {


    public static void main(String[] args) {
        // 使用Java自带线程池，生产环境不推荐使用，这里演示demo为了简单

        ExecutorService threadPool = Executors.newFixedThreadPool(5);
        // 需要发送 20个短信
        CountDownLatch countDownLatch = new CountDownLatch(20);
        AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < 20; i++) {
            threadPool.execute(() -> {
                // 模拟发送短信
                System.out.println("发送短信");
                // 假装耗时1s
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 发送短信完成
                System.out.println("-----------发送短信数量:" + count.incrementAndGet());
                countDownLatch.countDown();
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("所有短信发送完成");

    }


}
