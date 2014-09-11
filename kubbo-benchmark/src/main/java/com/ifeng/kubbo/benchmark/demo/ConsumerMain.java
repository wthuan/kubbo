package com.ifeng.kubbo.benchmark.demo;

import akka.dispatch.OnComplete;
import com.ifeng.kubbo.Ref;
import com.ifeng.kubbo.akka.Context;
import com.ifeng.kubbo.akka.Reference;
import scala.concurrent.Future;

/**
 * <title>ConsumerMain</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-11
 */
public class ConsumerMain {

    public static void main(String[] args) throws InterruptedException {
        Ref ref = Reference.get();
        Echo echo = ref.getRef(Echo.class, null, null);

        Future<String> future = echo.asyncEcho("hello");

        future.onComplete(new OnComplete<String>(){

            @Override
            public void onComplete(Throwable failure, String success) throws Throwable {
                System.out.println("failure:"+failure);
                System.out.println("success:"+success);
            }
        }, Context.context());
        Thread.sleep(3000);
    }
}