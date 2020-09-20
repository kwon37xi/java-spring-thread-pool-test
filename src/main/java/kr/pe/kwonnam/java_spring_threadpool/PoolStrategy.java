package kr.pe.kwonnam.java_spring_threadpool;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
     * 사실상 ExecutorSerive의 cachedThreadPool 과 같다.
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
     * SpringFramework ThreadPoolTaskExecutor corePoolSize 1000, maxPoolSize : 정수최대, queueCapacity : 정수최대
     */
    THREAD_POOL_TASK_EXECUTOR_QUEUE_INTMAX {
        @Override
        public Executor getExecutor() {
            return createThreadPoolTaskExecutor(ThreadPoolTester.DEFAULT_CORE_POOL_SIZE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        @Override
        public void shutdown(Executor executor) {
            ((ThreadPoolTaskExecutor) executor).shutdown();
        }
    },
    /**
     * SpringFramework ThreadPoolTaskExecutor corePoolSize 1000, maxPoolSize : 정수최대, queueCapacity : 10
     * 작은 수의 queueCapacity는 0이랑 별로 차이 없음.
     */
    THREAD_POOL_TASK_EXECUTOR_MAX_INTMAX_QUEUE_10 {
        @Override
        public Executor getExecutor() {
            return createThreadPoolTaskExecutor(ThreadPoolTester.DEFAULT_CORE_POOL_SIZE, Integer.MAX_VALUE, 10);
        }

        @Override
        public void shutdown(Executor executor) {
            ((ThreadPoolTaskExecutor) executor).shutdown();
        }
    },
    /**
     * SpringFramework ThreadPoolTaskExecutor corePoolSize 1000, maxPoolSize : 1000, queueCapacity : 0
     */
    THREAD_POOL_TASK_EXECUTOR_MIN_MAX_SAME_QUEUE_0 {
        @Override
        public Executor getExecutor() {
            return createThreadPoolTaskExecutor(ThreadPoolTester.DEFAULT_CORE_POOL_SIZE, ThreadPoolTester.DEFAULT_CORE_POOL_SIZE, 0);
        }

        @Override
        public void shutdown(Executor executor) {
            ((ThreadPoolTaskExecutor) executor).shutdown();
        }
    };

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
