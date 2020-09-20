# Java 와 SpringFramework 의 Thread Pool 작동 방식 테스트
* Java의 `Executors.newCachedThreadPool()`과 `Executors.newFixedThreadPool(갯수)`
* SpringFramework 의 `ThreadPoolTaskExecutor` 설정에 따른 작동 방식 변화.
* 위 두가지를 실제 코드로 확인해보기.

## 최대 쓰레드 갯수
* [Java: What is the limit to the number of threads you can create? | Vanilla #Java](http://vanillajava.blogspot.com/2011/07/java-what-is-limit-to-number-of-threads.html)
* 위 문서에 따르면 64Bit Linux 에서 32,072 개의 쓰레드가 생성가능함.

## ThreadPoolTaskExecutor 를 CachedThreadPool 처럼 사용하는 방법
* `corePoolSize` : `0`
* `maxPoolSize` : `Integer.MAX_VALUE`
* `queueCapacity` : `0`

## ThreadPoolTaskExecutor 를 FixedThreadPool 처럼 사용하는 방법
*  `corePoolSize` : 원하는 고정 크기 쓰레드 갯수
* `maxPoolSize` : `Integer.MAX_VALUE`
* `queueCapacity` : `Integer.MAX_VALUE`
* 위와 같이 설정하면 실제로는 `corePoolSize` 만큼만 쓰레드가 생성된다.
* 만약 쓰레드가 적체되어 `corePoolSize` 이상의 작업이 들어오면 `queue` 에 `queueCapacity`만큼 들어가고,
 `corePool` 에 남는 자리가 생기면 `queue`에 있던것이 들어간다.
* `queueCapacity`도 꽉차면 그제서야 `maxPoolSize` 에 따라 Pool 의 쓰레드 총 갯수가 증가하기 시작한다.
  * `queueCapacity=Integer.MAX_VALUE`일 경우에는 여기까지 가기 힘들다.

## 결과 먼저
*  `Executors.newCachedThreadPool()` 혹은 `ThreadPoolTaskExecutor`를 CachedThreadPool과 유사하게 설정하면
 쓰레드의 작업이 적체될 경우 시스템 한계치에 달하는 쓰레드를 생성하다가 죽어버린다.
* 따라서, 
  * cachedThreadPool 이 필요한 경우
    * **명확하게 정말 빠르게 끝나는 task 만 할당하는게 확실할 경우**에는 cachedThreadPool 혹은 이에 준하는 설정이 낫다.
    * cachedThreadPool 은 항상 필요한 만큼만 쓰레드를 생성하고, 불필요해지면 자동으로 쓰레드를 반환하므로 최적 상태가 된다.
    * 지연이 발생할 가능성이 있다면 cachedThreadPool 의 경우 Java 프로세스가 수만개의 쓰레드를 생성하다가 죽을 수 있다.
  * 그 외의 대부분은 fixedThreadPool을 사용하는게 나아보인다.
      * `Executors.newFixedThreadPool(적당한쓰레드갯수)` 를 사용하거나, 
      * `ThreadPoolTaskExecutor`를 위에 설명한 대로 설정한다.
      * 단점은, 불필요하게 항상 고정 크기 쓰레드가 생성된 상태로 유지된다. 실제로 사용되지 않아도 유지된다.

## EXECUTOR_SERVICE_CACHED
* `Executors.newCachedThreadPool()` 사용.
```
./gradlew run --args="EXECUTOR_SERVICE_CACHED"
```
* 결과
  * 총 쓰레드 32600 개를 생성하고 죽음. 가끔씩 task가 처리되는 시간에 따라
   안죽을 때도 있으나 쓰레드를 수만개 생성해서 메모리가 폭주하는 것은 마찬가지임.
  * 한개의 쓰레드도 작업을 마치지 못함.
  * `# after thread generation ...`, `# The end` 출력안됨. 즉, 쓰레드 생성 반복문을 마치지도 못했음.
* 결과 출력 
```
# current thread [pool-1-thread-32597] idx : 32596, current active thread count 32599
# current thread [pool-1-thread-32598] idx : 32597, current active thread count 32600
#
# There is insufficient memory for the Java Runtime Environment to continue.
# Native memory allocation (mmap) failed to map 12288 bytes for committing reserved memory.
# An error report file with more information is saved as:
# /home/kwon37xi/projects_kwon37xi/java-spring-thread-pool-test/hs_err_pid945971.log
[thread 140060855953152 also had an error]

OpenJDK 64-Bit Server VM warning: INFO: os::commit_memory(0x00007f58dad0b000, 12288, 0) failed; error='메모리를 할당할 수 없습니다' (errno=12)
Exception in thread "main" java.lang.OutOfMemoryError: unable to create new native thread
        at java.lang.Thread.start0(Native Method)
        at java.lang.Thread.start(Thread.java:717)
        at java.util.concurrent.ThreadPoolExecutor.addWorker(ThreadPoolExecutor.java:957)
        at java.util.concurrent.ThreadPoolExecutor.execute(ThreadPoolExecutor.java:1378)
        at kr.pe.kwonnam.java_spring_threadpool.ThreadPoolTester.main(ThreadPoolTester.java:43)
OpenJDK 64-Bit Server VM warning: Attempt to deallocate stack guard pages failed.
OpenJDK 64-Bit Server VM warning: INFO: os::commit_memory(0x00007f627580f000, 12288, 0) failed; error='메모리를 할당할 수 없습니다' (errno=12)

```
* 참고
  * `ExecutorService.newCachedThreadPool()` 는 `corePoolSize=0`, `maxPoolSize=Integer.MAX_VALUE`, workQueue 로 `SynchronousQueue`를 사용하는데,
  * 이는 항상 poll 해가는 쓰레드가 존재할 때만 insert 를 할 수 있는 큐이다(queue size를 항상 0으로 유지).
  * 즉, 비록 corePoolSize 가 0 이라해도 뭔가 쓰레드 생성을 요청하는 순간 queue 에 넣고 빼가는게 즉시 이뤄져서
  항상 필요한만큼의 쓰레드가 즉시 생성된다.
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
  * 쓰레드 생성 반복문 직후 찍는 `after thread generation ...`가 1000개의 쓰레드의 시작 실행 구문이 찍힌 뒤에
  출력된 것으로 보아, **workQueue 가 존재하여 이미 모든 쓰레드 정보는 queue에 다들어간 생태** 임을 알 수 있다.
  * 계속 `1001`개의 쓰레드 갯수를 유지했다.
  * 느리지만 끝까지 문제 없이 실행됐다.
* 참고 : `workQueue`
  * `newFixedThreadPool()` 은 내부적으로 `workQueue` 를 다음과 같이 생성하는데, 
  해당 코드를 보면 `Integer.MAX_VALUE` 크기의 큐를 생성하는 것을 볼 수 있다.

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
# current thread [pool-1-thread-999] idx : 998, current active thread count 1001
# current thread [pool-1-thread-1000] idx : 999, current active thread count 1001
# after thread generation ...
# current thread [pool-1-thread-2] idx : 1, , current active thread count 1001, countDownLatch : 49999 END
# current thread [pool-1-thread-6] idx : 5, , current active thread count 1001, countDownLatch : 49998 END
....
# current thread [pool-1-thread-243] idx : 49996, , current active thread count 1001, countDownLatch : 3 END
# current thread [pool-1-thread-240] idx : 49997, , current active thread count 1001, countDownLatch : 2 END
# current thread [pool-1-thread-330] idx : 49998, , current active thread count 1001, countDownLatch : 1 END
# current thread [pool-1-thread-233] idx : 49999, , current active thread count 1001, countDownLatch : 0 END
# The end
```

## THREAD_POOL_TASK_EXECUTOR_CORE_POOL_SIZE_AND_QUEUE_0 : TODO test again
* `ThreadPoolTaskExecutor` == `Executors.newCachedThreadPool()` 유사한 설정
  * `corePoolSize` : `0` - 쓰레드 풀이 반환되면 일정 시간 기다렸다가 0개로 줄인다.
  * `maxPoolSize` : `Integer.MAX_VALUE`
  * `queueCapacity` : `0` - 큐가 없으므로 `maxPoolSize`만큼 즉각 증가시킨다.
```
# current thread [MYTHREADPOOL-32595] idx : 32594, current active thread count 32597
# current thread [MYTHREADPOOL-32596] idx : 32595, current active thread count 32598
# current thread [MYTHREADPOOL-32597] idx : 32596, current active thread count 32599
#
[thread 140089966102272 also had an error]# There is insufficient memory for the Java Runtime Environment to continue.

# Native memory allocation (mmap) failed to map 12288 bytes for committing reserved memory.
# An error report file with more information is saved as:
# /home/kwon37xi/projects_kwon37xi/java-spring-thread-pool-test/hs_err_pid987427.log
[thread 140131217250048 also had an error]
OpenJDK 64-Bit Server VM warning: Attempt to protect stack guard pages failed.
OpenJDK 64-Bit Server VM warning: Attempt to protect stack guard pages failed.
OpenJDK 64-Bit Server VM warning: Attempt to protect stack guard pages failed.
OpenJDK 64-Bit Server VM warning: Attempt to protect stack guard pages failed.
OpenJDK 64-Bit Server VM warning: Attempt to protect stack guard pages failed.
OpenJDK 64-Bit Server VM warning: Attempt to protect stack guard pages failed.
OpenJDK 64-Bit Server VM warning: Attempt to protect stack guard pages failed.
OpenJDK 64-Bit Server VM warning: INFO: os::commit_memory(0x00007f693caa9000, 12288, 0) failed; error='메모리를 할당할 수 없습니다' (errno=12)
OpenJDK 64-Bit Server VM warning: INFO: os::commit_memory(0x00007f693c9a8000, 12288, 0) failed; error='메모리를 할당할 수 없습니다' (errno=12)
Exception in thread "main" java.lang.OutOfMemoryError: unable to create new native thread
        at java.lang.Thread.start0(Native Method)
        at java.lang.Thread.start(Thread.java:717)
        at java.util.concurrent.ThreadPoolExecutor.addWorker(ThreadPoolExecutor.java:957)
        at java.util.concurrent.ThreadPoolExecutor.execute(ThreadPoolExecutor.java:1378)
        at org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor.execute(ThreadPoolTaskExecutor.java:321)
        at kr.pe.kwonnam.java_spring_threadpool.ThreadPoolTester.main(ThreadPoolTester.java:43)
OpenJDK 64-Bit Server VM warning: Attempt to deallocate stack guard pages failed.
OpenJDK 64-Bit Server VM warning: INFO: os::commit_memory(0x00007f72d75d2000, 12288, 0) failed; error='메모리를 할당할 수 없습니다' (errno=12)

```
* 결과
  * 총 쓰레드 32599 개를 생성하고 죽음.
  * 한개의 쓰레드도 작업을 마치지 못함.
  * `# after thread generation ...` 출력안됨. 즉, 쓰레드 생성 반복문을 마치지도 못했음.
  * `Executors.newCachedThreadPool()`과 동일한 결과

## THREAD_POOL_TASK_EXECUTOR_QUEUE_INTMAX
* `ThreadPoolTaskExecutor` == `Executors.newFixedThreadPool()` 유사한 설정
  * `corePoolSize` : `1000` - 쓰레드 풀 항상 1000개 유지
  * `maxPoolSize` : `Integer.MAX_VALUE`
  * `queueCapacity` : `Integer.MAX_VALUE`
* 결과
  * `idx: 999` 에서  `# after thread generation ...` 가 출렸됐다는 것은 이 시점에는 이미 
  쓰레드 풀 Queue 에 모든 요청이 들어갔다는 의미임.
  * 최대 쓰레드 갯수 1001개를 유지하면서 느리지만 모든 작업을 무사히 마침.
  * `Executors.newFixedThreadPool(1000)`와 동일한 결과
  
```
# current thread [MYTHREADPOOL-999] idx : 998, current active thread count 1001
# current thread [MYTHREADPOOL-1000] idx : 999, current active thread count 1001
# after thread generation ...
# current thread [MYTHREADPOOL-1] idx : 0, , current active thread count 1001, countDownLatch : 49999 END
# current thread [MYTHREADPOOL-1] idx : 1000, current active thread count 1001
....
# current thread [MYTHREADPOOL-490] idx : 49996, , current active thread count 1001, countDownLatch : 3 END
# current thread [MYTHREADPOOL-527] idx : 49997, , current active thread count 1001, countDownLatch : 2 END
# current thread [MYTHREADPOOL-411] idx : 49998, , current active thread count 1001, countDownLatch : 1 END
# current thread [MYTHREADPOOL-709] idx : 49999, , current active thread count 1001, countDownLatch : 0 END
# The end
```

## THREAD_POOL_TASK_EXECUTOR_MAX_INTMAX_QUEUE_40000
* `corePoolSize`, `maxPoolSize`, `queueCapacity`의 관계를 보여주는 예제.
  * `corePoolSize` : `1000`
  * `maxPoolSize` : `Integer.MAX_VALUE`
  * `queueCapacity` : `40,000`
* 결과
  * 쓰레드를 총 50,000개 생성하는데, `idx=999`까지만 쓰레드를 생성하다가 그 이후에는 queue에 넣다가
  * queue 가 꽉차는 41,000 개 째부터 더이상 queue에 넣을 수 없어서 `maxPoolSize=Integer.MAX_VALUE`에 따라 Thread를
  생성하기 시작하는 것을 볼 수 있다.
  * 쓰레드가 `총 생성 작업갯수(50,000)-queueSize(40,000)`즈음은 `10001`에 이르자, queue도 꽉차고 queue에 못넣은 것은 corePoolSize를 넘어서는 갯수의 쓰레드를 생성함으로써
  모두다 할당이 완료되었기 때문에 그 순간 `# after thread generation ...`이 출력되면서
  모든 작업을 threadPool에 넣는 것이 완료됨을 볼 수 있다.
  * 쓰레드 갯수가 한계 수치인 `30,000`개 이상까지 가지 않고 `10,001`에 계속 머물렀기 때문에 모든 작업을 완료하였다.
```
# current thread [MYTHREADPOOL-998] idx : 997, current active thread count 1000
# current thread [MYTHREADPOOL-999] idx : 998, current active thread count 1001
# current thread [MYTHREADPOOL-1000] idx : 999, current active thread count 1001
# current thread [MYTHREADPOOL-1001] idx : 41000, current active thread count 1003
# current thread [MYTHREADPOOL-1002] idx : 41001, current active thread count 1004
# current thread [MYTHREADPOOL-1003] idx : 41002, current active thread count 1005
....
# current thread [MYTHREADPOOL-9998] idx : 49997, current active thread count 10000
# current thread [MYTHREADPOOL-9999] idx : 49998, current active thread count 10001
# after thread generation ...
# current thread [MYTHREADPOOL-10000] idx : 49999, current active thread count 10001
# current thread [MYTHREADPOOL-1] idx : 0, , current active thread count 10001, countDownLatch : 49999 END
# current thread [MYTHREADPOOL-1] idx : 1000, current active thread count 10001
....
# current thread [MYTHREADPOOL-9737] idx : 40996, , current active thread count 10001, countDownLatch : 4 END
# current thread [MYTHREADPOOL-9683] idx : 40997, , current active thread count 10001, countDownLatch : 3 END
# current thread [MYTHREADPOOL-9689] idx : 40978, , current active thread count 10001, countDownLatch : 2 END
# current thread [MYTHREADPOOL-9747] idx : 40998, , current active thread count 10001, countDownLatch : 1 END
# current thread [MYTHREADPOOL-9682] idx : 40999, , current active thread count 10001, countDownLatch : 0 END
# The end
```

## THREAD_POOL_TASK_EXECUTOR_MAX_LIMITED_SAME_QUEUE_0
* `maxPoolSize`에 다다르면 예외가 발생함을 보여주는 예제. queue를 없애고, maxPoolSize를 작게 잡았다.
  * `corePoolSize` : `1000`
  * `maxPoolSize` : `2000` 
  * `queueCapacity` : `0`
* 결과
  * 2000개의 작업을 넣고나서 멈춰버림.
  * TODO : 왜 멈추나?