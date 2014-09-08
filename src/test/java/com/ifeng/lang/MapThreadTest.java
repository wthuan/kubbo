package com.ifeng.lang;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <title>MapThreadTest</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-7
 */
public class MapThreadTest {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        final Map<Object,Object> map = new HashMap<>();
        final AtomicInteger counter = new AtomicInteger();
        executor.execute(()->{
            for(;;){
                int c = counter.getAndIncrement();
                map.put(c,c);
            }
        });

        executor.execute(()->{
            for(;;){
                int c = counter.get();
                System.out.println(map.get(c));
            }
        });

        Thread.sleep(100000);
    }
}