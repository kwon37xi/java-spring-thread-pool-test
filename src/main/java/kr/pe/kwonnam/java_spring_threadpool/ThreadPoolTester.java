/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package kr.pe.kwonnam.java_spring_threadpool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 오래 실행되는 작업을 Thread Pool 에 넣었을 때 Pool 종류별 반응 살펴보기.
 * <p>
 * 첫번째 인자로 PoolStrategy enum 이름을 넣으면 된다.
 * <p>
 * Spring Framework 에 의존성이 걸려 있어야 한다.
 *
 * <pre>
 *     java ThreadPoolTester EXECUTOR_SERVICE_CACHED
 *     java ThreadPoolTester EXECUTOR_SERVICE_FIXED_1000
 *     java ThreadPoolTester THREAD_POOL_TASK_EXECUTOR_QUEUE_0
 *     java ThreadPoolTester THREAD_POOL_TASK_EXECUTOR_QUEUE_INTMAX
 *     java ThreadPoolTester THREAD_POOL_TASK_EXECUTOR_MAX_INTMAX_QUEUE_10
 *     java ThreadPoolTester THREAD_POOL_TASK_EXECUTOR_MIN_MAX_SAME_QUEUE_0
 *
 * </pre>
 */
public class ThreadPoolTester {

    public static final int TARGET_THREAD_COUNT = 50000;
    public static final int DEFAULT_CORE_POOL_SIZE = 1000;

    public static void main(String[] args) throws InterruptedException {
        String strategyName = Stream.of(args).findFirst().orElse(PoolStrategy.EXECUTOR_SERVICE_CACHED.name());
        PoolStrategy poolStrategy = PoolStrategy.valueOf(strategyName);

        System.out.printf("# Starting with Thread Pool %s%n", poolStrategy.name());
        Executor executor = poolStrategy.getExecutor();

        CountDownLatch countDownLatch = new CountDownLatch(TARGET_THREAD_COUNT);
        for (int idx = 0; idx < TARGET_THREAD_COUNT; idx++) {
            int value = idx;
            executor.execute(() -> {
                System.out.printf("# current thread [%s] idx : %d, current active thread count %d%n",
                        Thread.currentThread().getName(), value, Thread.activeCount());
                try {
                    // 5초만 지연돼도 컴퓨터 입장에서는 상당한 지연이다.
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                countDownLatch.countDown();
                System.out.printf("# current thread [%s] idx : %d, , current active thread count %d, countDownLatch : %d END%n",
                        Thread.currentThread().getName(), value, Thread.activeCount(), countDownLatch.getCount());
            });
        }

        System.out.println("# after thread generation ...");
        countDownLatch.await();
        System.out.println("# The end");
        poolStrategy.shutdown(executor);
    }
}