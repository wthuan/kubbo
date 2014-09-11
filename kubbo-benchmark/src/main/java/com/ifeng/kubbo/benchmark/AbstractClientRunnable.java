package com.ifeng.kubbo.benchmark;

import akka.dispatch.OnComplete;
import com.google.common.collect.Maps;
import com.ifeng.kubbo.akka.Context;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public abstract class AbstractClientRunnable implements ClientRunnable {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private CyclicBarrier barrier;

    private CountDownLatch latch;
    private final int MAX= 1024;
    private long             endTime;

    private boolean          running            = true;

    // response time spread
//    private Long[]           responseSpreads    = new Long[9];
    private Map<Integer,Long> responseSpreads = Maps.newHashMap();

    // error request per second
//    private long[]           errorTPS           = null;
//    private List<Long>          errorTPS        = new ArrayList<>(MAX);
    private Map<Integer,Long> errorTPS = Maps.newHashMap();


    // error response times per second
//    private long[]           errorResponseTimes = null;
//    private List<Long>          errorResponseTimes = new ArrayList<>(MAX);
      private Map<Integer,Long> errorResponseTimes = Maps.newHashMap();

    // tps per second
//    private long[]           tps                = null;
//    private List<Long>          tps             = new ArrayList<>(MAX);

    private Map<Integer,Long> tps = Maps.newHashMap();
    // response times per second
//    private long[]           responseTimes      = null;

//    private List<Long>          responseTimes = new ArrayList<>(MAX);
    private Map<Integer,Long> responseTimes = Maps.newHashMap();
    // benchmark startTime
    private long             startTime;

    // benchmark maxRange
//    private int              maxRange;

    private int requestNum;

    protected  Properties properties ;
    public AbstractClientRunnable(CyclicBarrier barrier, CountDownLatch latch,int requestNum) {
        this.properties = new Properties();
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("benchmark.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.barrier = barrier;
        this.latch = latch;
        this.requestNum = requestNum;
        this.startTime = System.nanoTime() /1000;
//        for(int i=0;i<MAX;i++){
//            errorTPS.add(0L);
//            errorResponseTimes.add(0L);
//            tps.add(0L);
//            responseTimes.add(0L);
//        }
//        for(int i=0;i<9;i++){
//            responseSpreads[i]=0L;
//        }

//        this.endTime = endTime;
//        maxRange = (Integer.parseInt(String.valueOf((endTime - startTime))) / 1000000) + 1;
//        errorTPS = new long[maxRange];
//        errorResponseTimes = new long[maxRange];
//        tps = new long[maxRange];
//        responseTimes = new long[maxRange];
        // init
//        for (int i = 0; i < maxRange; i++) {
//            errorTPS[i] = 0;
//            errorResponseTimes[i] = 0;
//            tps[i] = 0;
//            responseTimes[i] = 0;
//        }

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
        CountDownLatch platch = new CountDownLatch(requestNum);
        for (int i=0;i<requestNum;i++){
//        while (running) {
            long beginTime = System.nanoTime() / 1000L;
//            if (beginTime >= endTime) {
//                running = false;
//                break;
//            }
            try {
                Object result = invoke();
                if(result instanceof Future){
                    Future resultFuture = (Future) result;
                    resultFuture.onComplete(new OnComplete() {
                        @Override
                        public void onComplete(Throwable failure, Object success) throws Throwable {

                           count(beginTime,failure,success);
                            platch.countDown();
                        }
                    }, Context.context());
                }else{
                    count(beginTime,null,result);
                    platch.countDown();
                }

            } catch (Exception e) {
                LOGGER.error("client.invokeSync error", e);
                count(beginTime,e,null);
                platch.countDown();
            }
        }

        try {
            platch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void count(long beginTime,Throwable failure,Object success){
        long currentTime = System.nanoTime() / 1000L;
//        if (beginTime <= startTime) {
//            return;
//        }
        long consumeTime = currentTime - beginTime;
        sumResponseTimeSpread(consumeTime);
        int range = Integer.parseInt(String.valueOf(beginTime - startTime)) / 1000000;
//        if (range >= maxRange) {
//            System.err.println("benchmark range exceeds maxRange,range is: " + range + ",maxRange is: "
//                    + maxRange);
//            return;
//        }
        if (failure == null) {
//            tps.put(range,(Long)ObjectUtils.defaultIfNull(tps.get(range),0L) + 1L);
            tps.put(range,ObjectUtils.defaultIfNull(tps.get(range),0L)+1L);
            responseTimes.put(range, ObjectUtils.defaultIfNull(responseTimes.get(range), 0L) + consumeTime);
        } else {
            LOGGER.error("server error:"+failure);
            errorTPS.put(range, ObjectUtils.defaultIfNull(errorTPS.get(range), 0L) + 1L);
            errorResponseTimes.put(range, (Long) ObjectUtils.defaultIfNull(errorResponseTimes.get(range), 0L) + consumeTime);
        }
    }


    public abstract Object invoke();
    public Map<String,Object> getResults() {

        Map<String,Object> maps = Maps.newHashMap();
        maps.put("responseSpreads",responseSpreads);
        maps.put("tps",tps);
        maps.put("responseTimes",responseTimes);
        maps.put("errorTPS",errorTPS);
        maps.put("errorResponseTimes",errorResponseTimes);
//
//        List<Long[]> results = new ArrayList<Long[]>();
//        results.add(responseSpreads);
//        results.add(tps.values().toArray(new Long[0]));
//        results.add(responseTimes.values().toArray(new Long[0]));
//        results.add(errorTPS.values().toArray(new Long[0]));
//        results.add(errorResponseTimes.values().toArray(new Long[0]));
//        return results;
        return maps;
    }


    private void sumResponseTimeSpread(long responseTime) {
        responseTime = responseTime / 1000L;
        if (responseTime <= 0) {
            responseSpreads.put(0,ObjectUtils.defaultIfNull(responseSpreads.get(0),0L) + 1L);
        } else if (responseTime > 0 && responseTime <= 1) {
            responseSpreads.put(1,ObjectUtils.defaultIfNull(responseSpreads.get(1),0L) + 1L);
        } else if (responseTime > 1 && responseTime <= 5) {
            responseSpreads.put(2,ObjectUtils.defaultIfNull(responseSpreads.get(2),0L) + 1L);
        } else if (responseTime > 5 && responseTime <= 10) {
            responseSpreads.put(3,ObjectUtils.defaultIfNull(responseSpreads.get(3),0L) + 1L);
        } else if (responseTime > 10 && responseTime <= 50) {
            responseSpreads.put(4,ObjectUtils.defaultIfNull(responseSpreads.get(4),0L) + 1L);
        } else if (responseTime > 50 && responseTime <= 100) {
            responseSpreads.put(5,ObjectUtils.defaultIfNull(responseSpreads.get(5),0L) + 1L);
        } else if (responseTime > 100 && responseTime <= 500) {
            responseSpreads.put(6,ObjectUtils.defaultIfNull(responseSpreads.get(6),0L) + 1L);
        } else if (responseTime > 500 && responseTime <= 1000) {
            responseSpreads.put(7,ObjectUtils.defaultIfNull(responseSpreads.get(7),0L) + 1L);
        } else if (responseTime > 1000) {
            responseSpreads.put(8,ObjectUtils.defaultIfNull(responseSpreads.get(8),0L) + 1L);
        }
    }


}