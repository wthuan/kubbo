package com.ifeng.kubbo.remote.benchmark;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * <title>TestServiceClientRunnable</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-10
 */
public class TestServiceClientRunnable extends AbstractClientRunnable {


//    private Ref ref = Reference.get("akka.tcp://127.0.0.1:5005",5002);

    public TestServiceClientRunnable(CyclicBarrier barrier, CountDownLatch latch, long startTime, long endTime) {
        super(barrier, latch, startTime, endTime);
    }

    @Override
    public Object invoke() {
        System.out.println("run");
        return null;
    }
}