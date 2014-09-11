package com.ifeng.kubbo.benchmark.demo;


import com.ifeng.kubbo.akka.Reference;
import com.ifeng.kubbo.benchmark.AbstractClientRunnable;
import scala.concurrent.Future;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;


public class EchoClientRunnable extends AbstractClientRunnable {


    private static Echo echo = Reference.get().getRef(Echo.class,null,null);
    private int paramLength;
    private String param;
    public EchoClientRunnable(CyclicBarrier barrier, CountDownLatch latch, int requestNum) {
        super(barrier, latch,requestNum);
        String length = properties.getProperty("paramLength");
        this.paramLength = length == null ? 1 : Integer.parseInt(length);
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<paramLength;i++){
            sb.append('a');
        }
        this.param = sb.toString();
    }



    private Object syncInvoke(){
        String ret = echo.syncEcho(param);
        return ret;
    }

    private Object asyncInvoke() {
        Future<String> future = echo.asyncEcho(param);
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