package com.ifeng.kubbo.remote.benchmark.demo;

import com.ifeng.kubbo.remote.Ref;
import com.ifeng.kubbo.remote.akka.Reference;
import com.ifeng.kubbo.remote.benchmark.AbstractClientRunnable;
import scala.concurrent.Future;

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
public class EchoClientRunnable extends AbstractClientRunnable {


    private Ref ref = Reference.get();

    public EchoClientRunnable(CyclicBarrier barrier, CountDownLatch latch, long startTime, long endTime) {
        super(barrier, latch, startTime, endTime);
    }



    private Object syncInvoke(){
        Echo echo = ref.getRef(Echo.class, null, null);
        String ret = echo.syncEcho("aaaaa");
        System.out.println(ret);
        return ret;
    }

    private Object asyncInvoke() {
        Echo echo = ref.getRef(Echo.class, null, null);
        Future<String> future = echo.asyncEcho("aaaa");
        return future;
    }
    @Override
    public Object invoke() {
        if ("true".equals(properties.getProperty("async"))) {
            return asyncInvoke();
        }else {
            return syncInvoke();
        }

    }
}