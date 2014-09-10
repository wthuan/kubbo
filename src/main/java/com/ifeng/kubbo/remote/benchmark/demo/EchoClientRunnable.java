package com.ifeng.kubbo.remote.benchmark.demo;

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


    private static Echo echo = Reference.get().getRef(Echo.class,null,null);

    public EchoClientRunnable(CyclicBarrier barrier, CountDownLatch latch, int requestNum) {
        super(barrier, latch,requestNum);
    }



    private Object syncInvoke(){
        String ret = echo.syncEcho("aaaaa");
        return ret;
    }

    private Object asyncInvoke() {
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