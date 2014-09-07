package com.ifeng.kubbo.remote.akka;

import akka.actor.ActorSystem;
import com.ifeng.TestService;
import com.ifeng.TestServiceImpl;
import com.ifeng.kubbo.remote.Reference;
import com.typesafe.config.ConfigFactory;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <title>ReferenceInBoxTest</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-5
 */
public class ReferenceInBoxTest extends TestCase {


    private ProviderContainer startProvider(){
        ActorSystem system = ActorSystem.create("kubbo", ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 1111)
                .withFallback(ConfigFactory.load()));
        ProviderContainer container = new ProviderContainer(system, 8);
        container.start(TestService.class, new TestServiceImpl(), null, null);
        System.out.println("servcie start");
        return container;
    }

    private Reference startReference() {
        ReferenceInbox reference = ReferenceInbox.get("akka.tcp://kubbo@127.0.0.1:1111", 2222);
        return reference;
    }
    @Test
    public void testGetRef() throws Exception {
        ProviderContainer container = startProvider();
        Thread.sleep(3000);
        Reference reference = startReference();
        System.out.println("waiting for register");
        Thread.sleep(10000);
        TestService ref = reference.getRef(TestService.class, null, null);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 100; i++) {
            executor.execute(() -> {
                try {
                    ref.testReturnInt(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        Thread.sleep(10000000);

    }
}