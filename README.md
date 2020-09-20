# Java 와 SpringFramework 의 Thread Pool 작동 방식 테스트
* Java의 `Executors.newCachedThreadPool()`과 `Executors.newFixedThreadPool(갯수)`
* SpringFramework 의 `ThreadPoolTaskExecutor` 설정에 따른 작동 방식 변화.
* 위 두가지를 실제 코드로 확인해보기.

## 최대 쓰레드 갯수
* [Java: What is the limit to the number of threads you can create? | Vanilla #Java](http://vanillajava.blogspot.com/2011/07/java-what-is-limit-to-number-of-threads.html)
* 위 문서에 따르면 64Bit Linux 에서 32,072 개의 쓰레드가 생성가능함.

## ThreadPoolTaskExecutor 를 CachedThreadPool 처럼 사용하는 방법
* `corePoolSize` : 적당히 지정
* `maxPoolSize` : `Integer.MAX_VALUE`
* `queueCapacity` : `0` 혹은 작은 숫자.

## ThreadPoolTaskExecutor 를 FixedThreadPool 처럼 사용하는 방법
*  `corePoolSize` : 적당한쓰레드갯수
* `maxPoolSize` : `Integer.MAX_VALUE`
* `queueCapacity` : `Integer.MAX_VALUE`
* 위와 같이 설정하면 실제로는 `corePoolSize` 만큼만 쓰레드가 생성된다.
* 만약 쓰레드가 적체되어 `corePoolSize` 이상의 작업이 들어오면 `queue` 에 `queueCapacity`만큼 들어가고, `corePool` 에 남는 자리가 생기면 `queue`에 있던것이 들어간다.
* `queueCapacity`도 꽉차면 그제서야 `maxPoolSize` 에 따라 Pool 의 쓰레드 총 갯수가 증가하기 시작한다.
  * `queueCapacity=Integer.MAX_VALUE`일 경우에는 여기까지 가기 힘들다.
* 실험상으로 봤을 때 `Executors.newFixedThreadPool()`에도 queue 가 존재하는 것으로 보임.

## 결과 먼저
*  `Executors.newCachedThreadPool()` 혹은 `ThreadPoolTaskExecutor`를 CachedThreadPool과 유사하게 설정하면
 쓰레드의 작업이 적체될 경우 시스템 한계치에 달하는 쓰레드를 생성하다가 죽어버린다.
* 따라서, 
  * `Executors.newFixedThreadPool(적당한쓰레드갯수)` 를 사용하거나, 
  * `ThreadPoolTaskExecutor` 는 다음과 같이 설정한다.

## EXECUTOR_SERVICE_CACHED
* `Executors.newCachedThreadPool()` 사용.
```
./gradlew run --args="EXECUTOR_SERVICE_CACHED"
```
* 결과
  * 총 쓰레드 32600 개를 생성하고 죽음.
  * 한개의 쓰레드도 작업을 마치지 못함.
  * `# after thread generation ...`, `# The end` 출력안됨. 즉, 쓰레드 생성 반복문을 마치지도 못했음.
* 결과 출력 
```
# current thread [pool-1-thread-32598] idx : 32597, current active thread count 32600
#
# There is insufficient memory for the Java Runtime Environment to continue.
# Native memory allocation (mmap) failed to map 12288 bytes for committing reserved memory.
# An error report file with more information is saved as:
# /home/kwon37xi/projects_kwon37xi/java-spring-thread-pool-test/hs_err_pid417759.log
[thread 139651156645632 also had an error]


OpenJDK 64-Bit Server VM warning: Attempt to protect stack guard pages failed.
   OpenJDK 64-Bit Server VM warning: Attempt to deallocate stack guard pages failed.
   OpenJDK 64-Bit Server VM warning: INFO: os::commit_memory(0x00007ef976d0b000, 12288, 0) failed; error='메모리를 할당할 수 없습니다' (errno=12)
   Exception in thread "main" java.lang.OutOfMemoryError: unable to create new native thread
           at java.lang.Thread.start0(Native Method)
           at java.lang.Thread.start(Thread.java:717)
           at java.util.concurrent.ThreadPoolExecutor.addWorker(ThreadPoolExecutor.java:957)
           at java.util.concurrent.ThreadPoolExecutor.execute(ThreadPoolExecutor.java:1378)
           at kr.pe.kwonnam.java_spring_threadpool.ThreadPoolTester.main(ThreadPoolTester.java:43)
   OpenJDK 64-Bit Server VM warning: Attempt to deallocate stack guard pages failed.
   OpenJDK 64-Bit Server VM warning: INFO: os::commit_memory(0x00007f031185a000, 12288, 0) failed; error='메모리를 할당할 수 없습니다' (errno=12)
```
* 참고
  * `ExecutorService.newCachedThreadPool()` 는 `corePoolSize=0`, `maxPoolSize=Integer.MAX_VALUE`, workQueue 로 `SynchronousQueue`를 사용하는데,
  * 이는 항상 poll 해가는 쓰레드가 존재할 때만 insert 를 할 수 있는 큐이다(queue size를 항상 0으로 유지).
  * 즉, 비록 corePoolSize 가 0 이라해도 뭔가 쓰레드 생성을 요청하는 순간 queue 에 넣고 빼가는게 즉시 이뤄져서
  쓰레드가 생성된다.
```
return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                              60L, TimeUnit.SECONDS,
                              new SynchronousQueue<Runnable>());
```
## EXECUTOR_SERVICE_FIXED_1000
* `Executors.newFixedThreadPool(1000)` 사용.
```
./gradlew run --args="EXECUTOR_SERVICE_FIXED_1000" 
```
* 결과
  * 쓰레드 생성 반복문 직후 찍는 `after thread generation ...`가 100개의 쓰레드의 시작 실행 구문이 찍힌 뒤에
  출력된 것으로 보아, **queue 가 존재하여 이미 모든 쓰레드 정보는 queue에 다들어간 생태** 로 보인다.
  * 계속 `1001`개의 쓰레드 갯수를 유지했다.
  * 느리지만 끝까지 실행됐다.
* 참고 : workQueue
  * `newFixedThreadPool()` 은 내부적으로 workQueue 를 다음과 같이 생성하는데, 해당 코드를 보면 `Integer.MAX_VALUE` 크기의 큐를 생성하는 것을 볼 수 있다.

```
// java.util.concurrent.LinkedBlockingQueue.LinkedBlockingQueue()

    /**
     * Creates a {@code LinkedBlockingQueue} with a capacity of
     * {@link Integer#MAX_VALUE}.
     */
    public LinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }
```
```
# current thread [pool-1-thread-998] idx : 997, current active thread count 1000
# current thread [pool-1-thread-999] idx : 998, current active thread count 1001
# current thread [pool-1-thread-1000] idx : 999, current active thread count 1001
# after thread generation ...
# current thread [pool-1-thread-1] idx : 0, countDownLatch : 49999 END
# current thread [pool-1-thread-5] idx : 4, countDownLatch : 49998 END

....
# current thread [pool-1-thread-984] idx : 49995, countDownLatch : 1 END
# current thread [pool-1-thread-978] idx : 49984, countDownLatch : 13 END
# current thread [pool-1-thread-981] idx : 49987, countDownLatch : 15 END
# current thread [pool-1-thread-962] idx : 49982, countDownLatch : 17 END
# The end
# current thread [pool-1-thread-997] idx : 49999, countDownLatch : 0 END
# current thread [pool-1-thread-995] idx : 49998, countDownLatch : 2 END
# current thread [pool-1-thread-972] idx : 49997, countDownLatch : 3 END
...
```

## THREAD_POOL_TASK_EXECUTOR_CORE_POOL_SIZE_AND_QUEUE_0 : TODO test again
* `ThreadPoolTaskExecutor` == `Executors.newCachedThreadPool()` 유사한 설정
  * `corePoolSize` : `0` - 쓰레드 풀이 반환되면 일정 시간 기다렸다가 0개로 줄인다.
  * `maxPoolSize` : `Integer.MAX_VALUE`
  * `queueCapacity` : `0` - 큐가 없으므로 `maxPoolSize`만큼 즉각 증가시킨다.
```
# current thread [MYTHREADPOOL-32590] idx : 32589, current active thread count 32592
# current thread [MYTHREADPOOL-32591] idx : 32590, current active thread count 32593
#
# There is insufficient memory for the Java Runtime Environment to continue.
# Native memory allocation (mmap) failed to map 12288 bytes for committing reserved memory.
# An error report file with more information is saved as:
# /home/kwon37xi/projects_kwon37xi/java-spring-thread-pool-test/hs_err_pid470837.log
[thread 140693321680640 also had an error]

OpenJDK 64-Bit Server VM warning: INFO: os::commit_memory(0x00007fec1d0af000, 12288, 0) failed; error='메모리를 할당할 수 없습니다' (errno=12)
Exception in thread "main" java.lang.OutOfMemoryError: unable to create new native thread
        at java.lang.Thread.start0(Native Method)
        at java.lang.Thread.start(Thread.java:717)
        at java.util.concurrent.ThreadPoolExecutor.addWorker(ThreadPoolExecutor.java:957)
        at java.util.concurrent.ThreadPoolExecutor.execute(ThreadPoolExecutor.java:1378)
        at org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor.execute(ThreadPoolTaskExecutor.java:321)
        at kr.pe.kwonnam.java_spring_threadpool.ThreadPoolTester.main(ThreadPoolTester.java:43)
OpenJDK 64-Bit Server VM warning: Attempt to deallocate stack guard pages failed.
OpenJDK 64-Bit Server VM warning: INFO: os::commit_memory(0x00007ff5b7661000, 12288, 0) failed; error='메모리를 할당할 수 없습니다' (errno=12)

```
* 결과
  * 총 쓰레드 32600 개를 생성하고 죽음.
  * 한개의 쓰레드도 작업을 마치지 못함.
  * `# after thread generation ...`, `# The end` 출력안됨. 즉, 쓰레드 생성 반복문을 마치지도 못했음.


## THREAD_POOL_TASK_EXECUTOR_QUEUE_INTMAX
* `ThreadPoolTaskExecutor` == `Executors.newFixedThreadPool()` 유사한 설정
  * `corePoolSize` : `1000` - 쓰레드 풀 항상 1000개 유지
  * `maxPoolSize` : `Integer.MAX_VALUE`
  * `queueCapacity` : `Integer.MAX_VALUE`
* 결과
  * `idx: 999` 에서  `# after thread generation ...` 가 출렸됐다는 것은 이 시점에는 이미 쓰레드 풀 Queue 에 모든 요청이 들어갔다는 의미임.
  * 최대 쓰레드 갯수 1001개를 유지하면서 느리지만 모든 작업을 무사히 마침.
  
```
# current thread [MYTHREADPOOL-999] idx : 998, current active thread count 1001
# current thread [MYTHREADPOOL-1000] idx : 999, current active thread count 1001
# after thread generation ...

# current thread [MYTHREADPOOL-1] idx : 0, countDownLatch : 49999 END
# current thread [MYTHREADPOOL-4] idx : 3, countDownLatch : 49998 END
# current thread [MYTHREADPOOL-1] idx : 1000, current active thread count 1001
....
# current thread [MYTHREADPOOL-608] idx : 49963, current active thread count 1001
# current thread [MYTHREADPOOL-737] idx : 49999, current active thread count 1001
# current thread [MYTHREADPOOL-17] idx : 49000, countDownLatch : 999 END
# current thread [MYTHREADPOOL-1] idx : 49001, countDownLatch : 998 END
....
# current thread [MYTHREADPOOL-800] idx : 49998, countDownLatch : 2 END
# current thread [MYTHREADPOOL-608] idx : 49963, countDownLatch : 1 END
# current thread [MYTHREADPOOL-737] idx : 49999, countDownLatch : 0 END
# The end
```