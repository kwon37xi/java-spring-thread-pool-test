package kr.pe.kwonnam.java_spring_threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class TooManyCachedThreadPoolTester {
    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 30_000; i++) {  // 35_000 으로 변경하면 시스템 Down, 그러나 쓰레드 생성 속도를 늦추면 괜찮음.
            ExecutorService executorService = Executors.newCachedThreadPool();
            executorService.submit(() -> {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            System.out.printf("newCachedThreadPool() idx %d - current Active Thread count %d%n", i, Thread.activeCount());
        }

        System.out.printf("Thread generation ended.%n");
        TimeUnit.MINUTES.sleep(2);
        System.out.printf("After 2 minutes current Active Thread count %d%n", Thread.activeCount());
    }
}
