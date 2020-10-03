package kr.pe.kwonnam.java_spring_threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TooManyFixedThreadPoolTester {
    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 30_000; i++) { // 35_000 으로 변경하면 시스템 Down
            ExecutorService executorService = Executors.newFixedThreadPool(1);
            executorService.submit(() -> {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            System.out.printf("newFiexedThreadPool(1) idx %d - current Active Thread count %d%n", i, Thread.activeCount());
        }

        System.out.printf("Thread generation ended.%n");
        TimeUnit.MINUTES.sleep(2);
        System.out.printf("After 2 minutes current Active Thread count %d%n", Thread.activeCount());
    }
}
