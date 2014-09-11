package com.ifeng.kubbo.benchmark;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.ObjectUtils;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;


public abstract class AbstractBenchmarkClient {


    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static long                   maxTPS     = 0;

    private static long                   minTPS     = 0;

    private static long                   allRequestSum;

    private static long                   allResponseTimeSum;

    private static long                   allErrorRequestSum;

    private static long                   allErrorResponseTimeSum;

//    private static int                    runtime;

    // < 0
    private static long                   below0sum;

    // (0,1]
    private static long                   above0sum;

    // (1,5]
    private static long                   above1sum;

    // (5,10]
    private static long                   above5sum;

    // (10,50]
    private static long                   above10sum;

    // (50,100]
    private static long                   above50sum;

    // (100,500]
    private static long                   above100sum;

    // (500,1000]
    private static long                   above500sum;

    // > 1000
    private static long                   above1000sum;

    Properties properties;

    public void run(String[] args) throws Exception {
        this.properties = new Properties();
        this.properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("benchmark.properties"));

        final int concurrents = Integer.parseInt(properties.getProperty("concurrents"));

//        runtime = Integer.parseInt(properties.getProperty("runtime"));
//        long warmTime = Integer.parseInt(properties.getProperty("warmRequest"));
        int requestNum = Integer.parseInt(properties.getProperty("requestNum"));
//        final long endtime = System.nanoTime() / 1000L + runtime * 1000 * 1000L;


        Date currentDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
//        calendar.add(Calendar.SECOND, runtime);
        StringBuilder startInfo = new StringBuilder(dateFormat.format(currentDate));
        startInfo.append(" ready to start client benchmark,server is ");
        startInfo.append("concurrents is: ").append(concurrents);
//        startInfo.append("warm time is: ").append(warmTime);
        startInfo.append(" s,the benchmark will end at:").append(dateFormat.format(calendar.getTime()));
        startInfo.append("\r\n----------------custom config---------\r\n");
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            startInfo.append(entry.getKey()).append("=").append(entry.getValue()).append("\r\n");
        }
        System.out.println(startInfo.toString());

        CyclicBarrier barrier = new CyclicBarrier(concurrents);
        CountDownLatch latch = new CountDownLatch(concurrents);
        List<ClientRunnable> runnables = new ArrayList<ClientRunnable>();
        // benchmark start after thirty seconds,let java app warm up
//        long beginTime = System.nanoTime() / 1000L;
        for (int i = 0; i < concurrents; i++) {
            ClientRunnable runnable = getClientRunnable(barrier, latch,requestNum);
            runnables.add(runnable);
        }

        startRunnables(runnables);

        latch.await();

        // read results & add all
        // key: runtime second range value: Long[2] array Long[0]: execute count Long[1]: response time sum
        Map<String, Long[]> times = new HashMap<String, Long[]>();
        Map<String, Long[]> errorTimes = new HashMap<String, Long[]>();

        Map<Integer,Long> tps_total = Maps.newHashMap();
        Map<Integer,Long> responseTimes_total= Maps.newHashMap();
        Map<Integer,Long> errorTPS_total= Maps.newHashMap();
        Map<Integer,Long> errorResponseTimes_total= Maps.newHashMap();

        for (ClientRunnable runnable : runnables) {
            Map<String,Object> result = runnable.getResults();
            Map<Integer,Long> responseSpreads = (Map<Integer,Long>)result.get("responseSpreads");
            below0sum += ObjectUtils.defaultIfNull(responseSpreads.get(0),0L);
            above0sum += ObjectUtils.defaultIfNull(responseSpreads.get(1),0L);
            above1sum += ObjectUtils.defaultIfNull(responseSpreads.get(2),0L);
            above5sum += ObjectUtils.defaultIfNull(responseSpreads.get(3),0L);
            above10sum += ObjectUtils.defaultIfNull(responseSpreads.get(4),0L);
            above50sum += ObjectUtils.defaultIfNull(responseSpreads.get(5),0L);
            above100sum += ObjectUtils.defaultIfNull(responseSpreads.get(6),0L);
            above500sum += ObjectUtils.defaultIfNull(responseSpreads.get(7),0L);
            above1000sum += ObjectUtils.defaultIfNull(responseSpreads.get(8),0L);
            Map<Integer,Long> tps = (Map<Integer,Long>)result.get("tps");
            Map<Integer,Long> responseTimes = (Map<Integer,Long>)result.get("responseTimes");
            Map<Integer,Long> errorTPS = (Map<Integer,Long>)result.get("errorTPS");
            Map<Integer,Long> errorResponseTimes = (Map<Integer,Long>)result.get("errorResponseTimes");
            for(Integer key : tps.keySet()){
                tps_total.put(key, ObjectUtils.defaultIfNull(tps_total.get(key),0L)+tps.get(key));
            }

            for(Integer key : responseTimes.keySet()){
                responseTimes_total.put(key,ObjectUtils.defaultIfNull(responseTimes_total.get(key),0L)+responseTimes.get(key));
            }

            for(Integer key : errorTPS.keySet()){
                errorTPS_total.put(key,ObjectUtils.defaultIfNull(errorTPS_total.get(key),0L)+errorTPS.get(key));
            }

            for(Integer key : errorResponseTimes.keySet()){
                errorResponseTimes_total.put(key,ObjectUtils.defaultIfNull(errorResponseTimes_total.get(key),0L)+errorResponseTimes.get(key));
            }


//            Long[] tps = results.get(1);
//            Long[] responseTimes = results.get(2);
//            Long[] errorTPS = results.get(3);
//            Long[] errorResponseTimes = results.get(4);
//            for (int i = 0; i < tps.length; i++) {
//                String key = String.valueOf(i);
//                if (times.containsKey(key)) {
//                    Long[] successInfos = times.get(key);
//                    Long[] errorInfos = errorTimes.get(key);
//                    successInfos[0] += tps[i];
//                    successInfos[1] += responseTimes[i];
//                    errorInfos[0] += errorTPS[i];
//                    errorInfos[1] += errorResponseTimes[i];
//                    times.put(key, successInfos);
//                    errorTimes.put(key, errorInfos);
//                } else {
//                    Long[] successInfos = new Long[2];
//                    successInfos[0] = tps[i];
//                    successInfos[1] = responseTimes[i];
//                    Long[] errorInfos = new Long[2];
//                    errorInfos[0] = errorTPS[i];
//                    errorInfos[1] = errorResponseTimes[i];
//                    times.put(key, successInfos);
//                    errorTimes.put(key, errorInfos);
//                }
//            }
        }

        long ignoreRequest = 0;
        long ignoreErrorRequest = 0;
//        int maxTimeRange = runtime - 30;
        // ignore the last 10 second requests,so tps can count more accurate
//        for (int i = 0; i < 10; i++) {
//            Long[] values = times.remove(String.valueOf(maxTimeRange - i));
//            if (values != null) {
//                ignoreRequest += values[0];
//            }
//            Long[] errorValues = errorTimes.remove(String.valueOf(maxTimeRange - i));
//            if (errorValues != null) {
//                ignoreErrorRequest += errorValues[0];
//            }
//        }

            allRequestSum = tps_total.values().stream().mapToLong(value -> value).sum();
            allResponseTimeSum = responseTimes_total.values().stream().mapToLong(value->value).sum();
            allErrorRequestSum = errorTPS_total.values().stream().mapToLong(value->value).sum();
            allErrorResponseTimeSum = errorResponseTimes_total.values().stream().mapToLong(value->value).sum();

            for(Integer key : errorTPS_total.keySet()){
                tps_total.put(key,ObjectUtils.defaultIfNull(tps_total.get(key),0L)+errorTPS_total.get(key));
            }

        for (Map.Entry<String, Long[]> entry : times.entrySet()) {
            long successRequest = entry.getValue()[0];
            long errorRequest = 0;
            if (errorTimes.containsKey(entry.getKey())) {
                errorRequest = errorTimes.get(entry.getKey())[0];
            }
            allRequestSum += successRequest;
            allResponseTimeSum += entry.getValue()[1];
            allErrorRequestSum += errorRequest;
            if (errorTimes.containsKey(entry.getKey())) {
                allErrorResponseTimeSum += errorTimes.get(entry.getKey())[1];
            }
            long currentRequest = successRequest + errorRequest;
            if (currentRequest > maxTPS) {
                maxTPS = currentRequest;
            }
            if (minTPS == 0 || currentRequest < minTPS) {
                minTPS = currentRequest;
            }
        }

            maxTPS = Collections.max(tps_total.values());
            minTPS = Collections.min(tps_total.values());
////        boolean isWriteResult = Boolean.parseBoolean(System.getProperty("write.statistics", "false"));
//        if (true) {
////            BufferedWriter writer = new BufferedWriter(new FileWriter("benchmark.all.results"));
//            for (Map.Entry<String, Long[]> entry : times.entrySet()) {
//                System.out.println(entry.getKey() + "," + entry.getValue()[0] + "," + entry.getValue()[1]);
//            }
//
//        }

        System.out.println("----------Benchmark Statistics--------------");
        System.out.println(" Concurrents: " + concurrents);
//        System.out.println(" ClientNums: " + clientNums);
//        System.out.println(" Runtime: " + runtime + " seconds");
        System.out.println(" Benchmark Time: " + tps_total.keySet().size());
        long benchmarkRequest = allRequestSum + allErrorRequestSum;
        long allRequest = benchmarkRequest + ignoreRequest + ignoreErrorRequest;
        System.out.println(" Requests: " + allRequest + " Success: " + (allRequestSum + ignoreRequest) * 100
                / allRequest + "% (" + (allRequestSum + ignoreRequest) + ") Error: "
                + (allErrorRequestSum + ignoreErrorRequest) * 100 / allRequest + "% ("
                + (allErrorRequestSum + ignoreErrorRequest) + ")");
        System.out.println(" Avg TPS: " + benchmarkRequest / tps_total.keySet().size() + " Max TPS: " + maxTPS
                + " Min TPS: " + minTPS);
        System.out.println(" Avg RT: " + (allErrorResponseTimeSum + allResponseTimeSum) / benchmarkRequest / 1000f
                + "ms");
        System.out.println(" RT <= 0: " + (below0sum * 100 / allRequest) + "% " + below0sum + "/" + allRequest);
        System.out.println(" RT (0,1]: " + (above0sum * 100 / allRequest) + "% " + above0sum + "/" + allRequest);
        System.out.println(" RT (1,5]: " + (above1sum * 100 / allRequest) + "% " + above1sum + "/" + allRequest);
        System.out.println(" RT (5,10]: " + (above5sum * 100 / allRequest) + "% " + above5sum + "/" + allRequest);
        System.out.println(" RT (10,50]: " + (above10sum * 100 / allRequest) + "% " + above10sum + "/" + allRequest);
        System.out.println(" RT (50,100]: " + (above50sum * 100 / allRequest) + "% " + above50sum + "/" + allRequest);
        System.out.println(" RT (100,500]: " + (above100sum * 100 / allRequest) + "% " + above100sum + "/" + allRequest);
        System.out.println(" RT (500,1000]: " + (above500sum * 100 / allRequest) + "% " + above500sum + "/"
                + allRequest);
        System.out.println(" RT > 1000: " + (above1000sum * 100 / allRequest) + "% " + above1000sum + "/" + allRequest);
        System.exit(0);
    }

    public abstract ClientRunnable getClientRunnable(CyclicBarrier barrier, CountDownLatch latch,
                                                     int requestNum) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException;

    protected void startRunnables(List<ClientRunnable> runnables) {
        for (int i = 0; i < runnables.size(); i++) {
            final ClientRunnable runnable = runnables.get(i);
            Thread thread = new Thread(runnable, "benchmarkclient-" + i);
            thread.start();
        }
    }

}