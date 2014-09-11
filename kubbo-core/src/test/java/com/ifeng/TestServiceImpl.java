package com.ifeng;

import akka.actor.ActorRef;
import akka.actor.TypedActor;
import akka.dispatch.Futures;
import scala.concurrent.Future;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <title>TestImpl</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-4
 */
public class TestServiceImpl implements TestService,TypedActor.Receiver {

    private static AtomicInteger counter = new AtomicInteger(0);
    private int id = counter.getAndIncrement();
    @Override
    public void testVoid(long sleep) throws Exception {
        System.out.println(Thread.currentThread().getName()+ " execute");
        if (sleep > 0) {
            Thread.sleep(sleep);

        }
    }

    @Override
    public int testReturnInt(long sleep)throws Exception {
        System.out.println(Thread.currentThread().getName() + " execute");
        if (sleep > 0) {
            Thread.sleep(sleep);

        }
        return 1;
    }

    @Override
    public Future testReturnFuture(long sleep)throws Exception {
        System.out.println(Thread.currentThread().getName()+" execute");
        if (sleep > 0) {
            Thread.sleep(sleep);

        }
        return Futures.successful(1);
    }

    @Override
    public int testReturnIntWithParam(int param) {
        System.out.println(Thread.currentThread().getName()+"[id="+this.id+"] execute "+param);
        return param;
    }


    @Override
    public void onReceive(Object message, ActorRef sender) {
        System.out.println(message);
    }
}