package kr.pe.kwonnam.java_spring_threadpool;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public enum PoolStrategy {
    /**
     * Java 기본 크기 무제한 Executors.newCachedThreadPool()
     */
    EXECUTOR_SERVICE_CACHED {
        @Override
        public Executor getExecutor() {
            return Executors.newCachedThreadPool();
        }

        @Override
        public void shutdown(Executor executor) {
            ((ExecutorService)executor).shutdownNow();
        }
    },
    /**
     * Java 기본 고정 크기 : Executors.newFixedThreadPool(1000)
     */
    EXECUTOR_SERVICE_FIXED_1000 {
        @Override
        public Executor getExecutor() {
            return Executors.newFixedThreadPool(ThreadPoolTester.DEFAULT_CORE_POOL_SIZE);
        }

        @Override
        public void shutdown(Executor executor) {
            ((ExecutorService)executor).shutdownNow();
        }
    },
    /**
     * SpringFramework ThreadPoolTaskExecutor corePoolSize 1000, maxPoolSize : 정수최대, queueCapacity : 0
     * 사실상 Executors.newCachedThreadPool() 과 같다.
     */
    THREAD_POOL_TASK_EXECUTOR_CORE_POOL_SIZE_AND_QUEUE_0 {
        @Override
        public Executor getExecutor() {
            return createThreadPoolTaskExecutor(0, Integer.MAX_VALUE, 0);
        }

        @Override
        public void shutdown(Executor executor) {
            ((ThreadPoolTaskExecutor) executor).shutdown();
        }
    },
    /**
     * SpringFramework ThreadPoolTaskExecutor corePoolSize 1000, maxPoolSize : corePoolSize 와 동일, queueCapacity : 정수최대
     * 사실상 Executors.newFixedThreadPool() 과 같다.
     */
    THREAD_POOL_TASK_EXECUTOR_QUEUE_INTMAX {
        @Override
        public Executor getExecutor() {
            return createThreadPoolTaskExecutor(ThreadPoolTester.DEFAULT_CORE_POOL_SIZE, ThreadPoolTester.DEFAULT_CORE_POOL_SIZE, Integer.MAX_VALUE);
        }

        @Override
        public void shutdown(Executor executor) {
            ((ThreadPoolTaskExecutor) executor).shutdown();
        }
    },
    /**
     * SpringFramework ThreadPoolTaskExecutor corePoolSize 1000, maxPoolSize : 정수최대, queueCapacity : 40000
     * <p/>
     * core/max/queueCapcity 의 관계 : core 갯수만큼 생성후 queue 를 넘어선 뒤에야 maxPoolSize 만큼 증가된다.
     *
     */
    THREAD_POOL_TASK_EXECUTOR_MAX_INTMAX_QUEUE_40000 {
        @Override
        public Executor getExecutor() {
            return createThreadPoolTaskExecutor(ThreadPoolTester.DEFAULT_CORE_POOL_SIZE, Integer.MAX_VALUE, 40000);
        }

        @Override
        public void shutdown(Executor executor) {
            ((ThreadPoolTaskExecutor) executor).shutdown();
        }
    },
    /**
     * SpringFramework ThreadPoolTaskExecutor corePoolSize 1000, maxPoolSize : 2000, queueCapacity : 0
     * <p/>
     * corePoolSize &lt; maxPoolSize, queueCapacity = 0 이면, maxPoolSize에 도달하는 순간 죽어버린다. 왜?
     */
    THREAD_POOL_TASK_EXECUTOR_MAX_LIMITED_QUEUE_0 {
        @Override
        public Executor getExecutor() {
            return createThreadPoolTaskExecutor(ThreadPoolTester.DEFAULT_CORE_POOL_SIZE, ThreadPoolTester.DEFAULT_CORE_POOL_SIZE * 2, 0);
        }

        @Override
        public void shutdown(Executor executor) {
            ((ThreadPoolTaskExecutor) executor).shutdown();
        }
    },
    /**
     * @see ForkJoinPool
     * @see ForkJoinPool#commonPool()
     *
     * <ul>
     *     <li>기본적으으로 프로세스 갯수만큼 쓰레드풀 생성함.</li>
     *     <li>java.util.concurrent.ForkJoinPool.common.parallelism - parallelism level : 풀 사이즈?</li>
     *     <li>java.util.concurrent.ForkJoinPool.common.threadFactory - the class name of a ForkJoinPool.ForkJoinWorkerThreadFactory</li>
     *     <li>java.util.concurrent.ForkJoinPool.common.exceptionHandler - the class name of a Thread.UncaughtExceptionHandler</li>
     * </ul>
     */
    FORK_JOIN_COMMON_POOL {
        @Override
        public Executor getExecutor() {
            System.out.printf("current java.util.concurrent.ForkJoinPool.common.parallelism : %d%n", System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism"));
            return ForkJoinPool.commonPool();
        }

        @Override
        public void shutdown(Executor executor) {
            // java.util.concurrent.ForkJoinPool.commonPool 에 따르면 System.exit() 시에 자동으로  shutDown 된다.
        }
    },
    /**
     * <pre>
     *      ./gradlew run --args="FORK_JOIN_COMMON_POOL_PARALLELISM_1000"
     * </pre>
     */
    FORK_JOIN_COMMON_POOL_PARALLELISM_1000 {
        @Override
        public Executor getExecutor() {
            System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1000");
            System.out.printf("current java.util.concurrent.ForkJoinPool.common.parallelism : %s%n", System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism"));
            return ForkJoinPool.commonPool();
        }

        @Override
        public void shutdown(Executor executor) {
            // java.util.concurrent.ForkJoinPool.commonPool 에 따르면 System.exit() 시에 자동으로  shutDown 된다.
        }
    }
    ;

    public abstract Executor getExecutor();
    public abstract void shutdown(Executor executor);

    public static Executor createThreadPoolTaskExecutor(int corePoolSize, int maxPoolSize, int queueCapacity) {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("MYTHREADPOOL-");
        threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
        threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
        threadPoolTaskExecutor.setQueueCapacity(queueCapacity);
        threadPoolTaskExecutor.setKeepAliveSeconds(60);
        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        threadPoolTaskExecutor.setAwaitTerminationSeconds(30);
        threadPoolTaskExecutor.afterPropertiesSet();
        return threadPoolTaskExecutor;
    }

}
