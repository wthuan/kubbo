package com.ifeng.kubbo.remote.akka;

import akka.actor.ActorSystem;
import akka.dispatch.OnComplete;
import com.ifeng.TestService;
import com.ifeng.TestServiceImpl;
import junit.framework.TestCase;
import org.junit.Test;
import scala.concurrent.Future;

import java.util.concurrent.CountDownLatch;


/**
 * <title>TYpedActorProviderCreatorTest</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-4
 */
public class TypedActorProviderFactoryTest extends TestCase{

    private int callNum = 100;

    private ActorSystem system;
    private TypedActorProviderFactory factory;
    @Override
    protected void setUp() throws Exception {
        this.system = ActorSystem.create();
        this.factory = new TypedActorProviderFactory(system,4);
    }

    @Test
    public void testCreate() throws Exception {
        TestService testService = factory.create(TestService.class, new TestServiceImpl(), null, null);
        final CountDownLatch countDownLatch = new CountDownLatch(callNum);

        for (int i = 0; i < callNum; i++) {
            Future future = testService.testReturnFuture(1000);
            future.onComplete(new OnComplete() {
                @Override
                public void onComplete(Throwable failure, Object success) throws Throwable {
                    countDownLatch.countDown();
                }
            },system.dispatcher());
        }

        countDownLatch.await();
    }

    @Test
    public void testIncreaseActor() throws Exception {
        TestService testService = this.factory.create(TestService.class, new TestServiceImpl(), null, null);
        this.factory.increaseActor(TestService.class, null, null);
        this.factory.increaseActor(TestService.class, null, null);
        this.factory.increaseActor(TestService.class, null, null);
        this.factory.increaseActor(TestService.class, null, null);

        final CountDownLatch countDownLatch = new CountDownLatch(callNum);

        for (int i = 0; i < callNum; i++) {
            Future future = testService.testReturnFuture(1000);
            future.onComplete(new OnComplete() {
                @Override
                public void onComplete(Throwable failure, Object success) throws Throwable {
                    countDownLatch.countDown();
                }
            },system.dispatcher());
        }

        countDownLatch.await();
    }



    @Test
    public void testDecreaseActor() throws Exception {
        TestService testService = this.factory.create(TestService.class, new TestServiceImpl(), null, null);
        this.factory.decreaseActor(TestService.class, null, null);
        this.factory.decreaseActor(TestService.class, null, null);
        this.factory.decreaseActor(TestService.class, null, null);
        final CountDownLatch countDownLatch = new CountDownLatch(callNum);

        for (int i = 0; i < callNum; i++) {
            Future future = testService.testReturnFuture(1000);
            future.onComplete(new OnComplete() {
                @Override
                public void onComplete(Throwable failure, Object success) throws Throwable {
                    countDownLatch.countDown();
                }
            },system.dispatcher());
        }

        countDownLatch.await();


    }


}