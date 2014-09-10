package com.ifeng.akka.future;

import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import akka.dispatch.OnSuccess;
import scala.concurrent.Future;

/**
 * <title>FutureTest</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-10
 */
public class FutureTest {

    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create();
        Future future = Futures.future(() -> 1, system.dispatcher());
        future.onSuccess(new OnSuccess() {
            @Override
            public void onSuccess(Object result) throws Throwable {
                System.out.println("first success run");
            }
        }, system.dispatcher());

        future.onComplete(new OnSuccess() {
            @Override
            public void onSuccess(Object result) throws Throwable {
                System.out.println("second success run");
            }
        }, system.dispatcher());


        Thread.sleep(Integer.MAX_VALUE);
    }

}