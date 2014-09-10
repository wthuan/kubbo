package com.ifeng.kubbo.remote.benchmark;

import akka.dispatch.OnComplete;
import com.ifeng.kubbo.remote.Ref;
import com.ifeng.kubbo.remote.akka.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public abstract class AbstractClientRunnable implements ClientRunnable {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private CyclicBarrier barrier;

    private CountDownLatch latch;

    private long             endTime;

    private boolean          running            = true;

    // response time spread
    private long[]           responseSpreads    = new long[9];

    // error request per second
    private long[]           errorTPS           = null;

    // error response times per second
    private long[]           errorResponseTimes = null;

    // tps per second
    private long[]           tps                = null;

    // response times per second
    private long[]           responseTimes      = null;

    // benchmark startTime
    private long             startTime;

    // benchmark maxRange
    private int              maxRange;


    private Ref ref;

    protected  Properties properties ;
    public AbstractClientRunnable(CyclicBarrier barrier, CountDownLatch latch, long startTime, long endTime) {
        this.properties = new Properties();
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("benchmark.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.barrier = barrier;
        this.latch = latch;
        this.startTime = startTime;
        this.endTime = endTime;
        maxRange = (Integer.parseInt(String.valueOf((endTime - startTime))) / 1000000) + 1;
        errorTPS = new long[maxRange];
        errorResponseTimes = new long[maxRange];
        tps = new long[maxRange];
        responseTimes = new long[maxRange];
        // init
        for (int i = 0; i < maxRange; i++) {
            errorTPS[i] = 0;
            errorResponseTimes[i] = 0;
            tps[i] = 0;
            responseTimes[i] = 0;
        }

    }


    @Override
    public void run() {
        try {
            barrier.await();
        } catch (Exception e) {
            // IGNORE
        }
        runJavaAndHessian();
        latch.countDown();
    }


    private void runJavaAndHessian() {
        while (running) {
            long beginTime = System.nanoTime() / 1000L;
            if (beginTime >= endTime) {
                running = false;
                break;
            }
            try {
                Object result = invoke();
                if(result instanceof Future){
                    Future resultFuture = (Future) result;
                    resultFuture.onComplete(new OnComplete() {
                        @Override
                        public void onComplete(Throwable failure, Object success) throws Throwable {

                           count(beginTime,failure,success);
                        }
                    }, Context.context());
                }else{
                    count(beginTime,null,result);
                }


            } catch (Exception e) {
                LOGGER.error("client.invokeSync error", e);
                long currentTime = System.nanoTime() / 1000L;
                if (beginTime <= startTime) {
                    continue;
                }
                long consumeTime = currentTime - beginTime;
                sumResponseTimeSpread(consumeTime);
                int range = Integer.parseInt(String.valueOf(beginTime - startTime)) / 1000000;
                if (range >= maxRange) {
                    System.err.println("benchmark range exceeds maxRange,range is: " + range + ",maxRange is: "
                            + maxRange);
                    continue;
                }
                errorTPS[range] = errorTPS[range] + 1;
                errorResponseTimes[range] = errorResponseTimes[range] + consumeTime;
            }
        }
    }


    private void count(long beginTime,Throwable failure,Object success){
        long currentTime = System.nanoTime() / 1000L;
        if (beginTime <= startTime) {
            return;
        }
        long consumeTime = currentTime - beginTime;
        sumResponseTimeSpread(consumeTime);
        int range = Integer.parseInt(String.valueOf(beginTime - startTime)) / 1000000;
        if (range >= maxRange) {
            System.err.println("benchmark range exceeds maxRange,range is: " + range + ",maxRange is: "
                    + maxRange);
            return;
        }
        if (failure == null) {
            tps[range] = tps[range] + 1;
            responseTimes[range] = responseTimes[range] + consumeTime;
        } else {
            LOGGER.error("server return result is null");
            errorTPS[range] = errorTPS[range] + 1;
            errorResponseTimes[range] = errorResponseTimes[range] + consumeTime;
        }
    }


    public abstract Object invoke();
    public List<long[]> getResults() {
        List<long[]> results = new ArrayList<long[]>();
        results.add(responseSpreads);
        results.add(tps);
        results.add(responseTimes);
        results.add(errorTPS);
        results.add(errorResponseTimes);
        return results;
    }

    private void sumResponseTimeSpread(long responseTime) {
        responseTime = responseTime / 1000L;
        if (responseTime <= 0) {
            responseSpreads[0] = responseSpreads[0] + 1;
        } else if (responseTime > 0 && responseTime <= 1) {
            responseSpreads[1] = responseSpreads[1] + 1;
        } else if (responseTime > 1 && responseTime <= 5) {
            responseSpreads[2] = responseSpreads[2] + 1;
        } else if (responseTime > 5 && responseTime <= 10) {
            responseSpreads[3] = responseSpreads[3] + 1;
        } else if (responseTime > 10 && responseTime <= 50) {
            responseSpreads[4] = responseSpreads[4] + 1;
        } else if (responseTime > 50 && responseTime <= 100) {
            responseSpreads[5] = responseSpreads[5] + 1;
        } else if (responseTime > 100 && responseTime <= 500) {
            responseSpreads[6] = responseSpreads[6] + 1;
        } else if (responseTime > 500 && responseTime <= 1000) {
            responseSpreads[7] = responseSpreads[7] + 1;
        } else if (responseTime > 1000) {
            responseSpreads[8] = responseSpreads[8] + 1;
        }
    }


}