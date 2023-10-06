# quarkus-reproducer-36172

This is reproducer project for issue [https://github.com/quarkusio/quarkus/issues/36172](https://github.com/quarkusio/quarkus/issues/36172).

It is not mandatory to add records in redis to reproduce the issue.

In parallel to the tests, we can run a redis [MONITOR command](https://redis.io/commands/monitor/) from a redis cli. 
In any test case, the output shows a scan command or a smembers command depending on the request URL.


## JVM mode/quarkus:dev

In JVM mode and in quarkus:dev mode, application behaves as expected. It serves the expected response.

Run the application in quarkus dev mode

```shell script
./mvnw package quarkus:dev
```

or in default packaging mode

```shell script
./mvnw clean package && java -jar target/quarkus-app/quarkus-run.jar
```


For both requests, the server serves a response with an empty array as expected.

```shell script
 curl -v http://127.0.0.1:8080/api/ids
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying 127.0.0.1:8080...
* Connected to 127.0.0.1 (127.0.0.1) port 8080 (#0)
> GET /api/ids HTTP/1.1
> Host: 127.0.0.1:8080
> User-Agent: curl/7.88.1
> Accept: */*
>
< HTTP/1.1 200 OK
< content-length: 2
< Content-Type: application/json;charset=UTF-8
<
{ [2 bytes data]
100     2  100     2    0     0      7      0 --:--:-- --:--:-- --:--:--     7[]
* Connection #0 to host 127.0.0.1 left intact

```

```shell script
 curl -v http://127.0.0.1:8080/api/ids?type=x
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying 127.0.0.1:8080...
* Connected to 127.0.0.1 (127.0.0.1) port 8080 (#0)
> GET /api/ids?type=x HTTP/1.1
> Host: 127.0.0.1:8080
> User-Agent: curl/7.88.1
> Accept: */*
>
< HTTP/1.1 200 OK
< content-length: 2
< Content-Type: application/json;charset=UTF-8
<
{ [2 bytes data]
100     2  100     2    0     0     72      0 --:--:-- --:--:-- --:--:--    74[]
* Connection #0 to host 127.0.0.1 left intact

```



## Native mode => issue

Build the quarkus native executable and its docker image:

```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true && docker build -f src/main/docker/Dockerfile.native -t quarkus/code-with-quarkus .
```

Run the quarkus native docker image (change the redis IP address):

```shell script
docker run -i --rm -p 8080:8080 -e QUARKUS_REDIS_HOSTS=redis://192.168.0.17:6379 quarkus/code-with-quarkus
```


Now both curl commands fail with a timeout at server side

```shell script
curl -v http://127.0.0.1:8080/api/ids
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying 127.0.0.1:8080...
* Connected to 127.0.0.1 (127.0.0.1) port 8080 (#0)
> GET /api/ids HTTP/1.1
> Host: 127.0.0.1:8080
> User-Agent: curl/7.88.1
> Accept: */*
>
  0     0    0     0    0     0      0      0 --:--:--  0:00:29 --:--:--     0< HTTP/1.1 500 Internal Server Error
< content-type: application/json; charset=utf-8
< content-length: 72
<
{ [72 bytes data]
100    72  100    72    0     0      2      0  0:00:36  0:00:30  0:00:06    19{"details":"Error id ffe1f213-af0b-4af1-b842-537dbafcc865-1","stack":""}
* Connection #0 to host 127.0.0.1 left intact

```

```
curl -v http://127.0.0.1:8080/api/ids?type=x
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying 127.0.0.1:8080...
* Connected to 127.0.0.1 (127.0.0.1) port 8080 (#0)
> GET /api/ids?type=x HTTP/1.1
> Host: 127.0.0.1:8080
> User-Agent: curl/7.88.1
> Accept: */*
>
  0     0    0     0    0     0      0      0 --:--:--  0:00:29 --:--:--     0< HTTP/1.1 500 Internal Server Error
< content-type: application/json; charset=utf-8
< content-length: 72
<
{ [72 bytes data]
100    72  100    72    0     0      2      0  0:00:36  0:00:30  0:00:06    20{"details":"Error id ffe1f213-af0b-4af1-b842-537dbafcc865-2","stack":""}
* Connection #0 to host 127.0.0.1 left intact
```



In both cases, the server exception is:

```
2023-10-06 17:19:26,422 ERROR [io.qua.ver.htt.run.QuarkusErrorHandler] (vert.x-eventloop-thread-2) HTTP Request to /api/ids failed, error id: ffe1f213-af0b-4af1-b842-537dbafcc865-1: (TIMEOUT,-1) Timed out after waiting 30000(ms) for a reply. address: __vertx.reply.1, repliedAddress: getIds
        at io.vertx.core.eventbus.impl.ReplyHandler.handle(ReplyHandler.java:76)
        at io.vertx.core.eventbus.impl.ReplyHandler.handle(ReplyHandler.java:24)
        at io.vertx.core.impl.VertxImpl$InternalTimerHandler.handle(VertxImpl.java:948)
        at io.vertx.core.impl.VertxImpl$InternalTimerHandler.handle(VertxImpl.java:919)
        at io.vertx.core.impl.EventLoopContext.emit(EventLoopContext.java:55)
        at io.vertx.core.impl.DuplicatedContext.emit(DuplicatedContext.java:179)
        at io.vertx.core.impl.ContextInternal.emit(ContextInternal.java:207)
        at io.vertx.core.impl.VertxImpl$InternalTimerHandler.run(VertxImpl.java:937)
        at io.netty.util.concurrent.PromiseTask.runTask(PromiseTask.java:98)
        at io.netty.util.concurrent.ScheduledFutureTask.run(ScheduledFutureTask.java:153)
        at io.netty.util.concurrent.AbstractEventExecutor.runTask(AbstractEventExecutor.java:174)
        at io.netty.util.concurrent.AbstractEventExecutor.safeExecute(AbstractEventExecutor.java:167)
        at io.netty.util.concurrent.SingleThreadEventExecutor.runAllTasks(SingleThreadEventExecutor.java:470)
        at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:569)
        at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
        at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
        at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
        at java.base@17.0.8/java.lang.Thread.run(Thread.java:833)
        at org.graalvm.nativeimage.builder/com.oracle.svm.core.thread.PlatformThreads.threadStartRoutine(PlatformThreads.java:807)
        at org.graalvm.nativeimage.builder/com.oracle.svm.core.posix.thread.PosixPlatformThreads.pthreadStartRoutine(PosixPlatformThreads.java:210)
```


