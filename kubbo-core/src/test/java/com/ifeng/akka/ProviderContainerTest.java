package com.ifeng.akka;

import akka.actor.ActorSystem;
import akka.dispatch.OnComplete;
import com.ifeng.TestService;
import com.ifeng.TestServiceImpl;
import com.ifeng.kubbo.akka.ProviderContainer;
import junit.framework.TestCase;
import org.junit.Test;
import scala.concurrent.Future;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * <title>TYpedActorProviderCreatorTest</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-4
 */
public class ProviderContainerTest extends TestCase{

    private int callNum = 100;

    private ActorSystem system;
    private ProviderContainer factory;
    @Override
    protected void setUp() throws Exception {
        this.system = ActorSystem.create();
        this.factory = new ProviderContainer(4);

    }

    @Test
    public void testCreate() throws Exception {
        TestService testService = factory.start(TestService.class, new TestServiceImpl(), null, null);
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
        TestService testService = this.factory.start(TestService.class, new TestServiceImpl(), null, null);
      this.factory.increaseActor(TestService.class,null,null);
      this.factory.increaseActor(TestService.class,null,null);
      this.factory.increaseActor(TestService.class,null,null);
      this.factory.increaseActor(TestService.class,null,null);
        this.factory.decreaseActor(TestService.class, null, null);
        this.factory.decreaseActor(TestService.class,null,null);

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
        TestService testService = this.factory.start(TestService.class, new TestServiceImpl(), null, null);
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

    @Test
    public void testSyncCall()throws Exception {
        TestService service = this.factory.start(TestService.class, new TestServiceImpl(), null, null);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(callNum);
        for (int i = 0; i < callNum; i++) {
            executor.execute(()-> {
                try {
                    service.testReturnInt(1000);
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        latch.await();

    }


}