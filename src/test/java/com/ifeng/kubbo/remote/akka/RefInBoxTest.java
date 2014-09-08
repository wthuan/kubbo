package com.ifeng.kubbo.remote.akka;

import akka.actor.ActorSystem;
import com.ifeng.TestService;
import com.ifeng.TestServiceImpl;
import com.ifeng.kubbo.remote.Ref;
import com.typesafe.config.ConfigFactory;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * <title>ReferenceInBoxTest</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-5
 */
public class RefInBoxTest extends TestCase {


    private ProviderContainer startProvider(){
        ActorSystem system = ActorSystem.create("kubbo", ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 1111)
                .withFallback(ConfigFactory.load()));
        ProviderContainer container = new ProviderContainer(system, 8);
        container.start(TestService.class, new TestServiceImpl(), null, null);
        System.out.println("servcie start");
        return container;
    }

    private Ref startReference() {
        RefInbox reference = RefInbox.get("akka.tcp://kubbo@127.0.0.1:1111", 2222);
        return reference;
    }
    @Test
    public void testGetRef() throws Exception {
        ProviderContainer container = startProvider();
        Thread.sleep(3000);
        Ref reference = startReference();
        TestService ref = reference.getRef(TestService.class, null, null);
        System.out.println("waiting...");
        for(;;){
            ref.testVoid(1000);
            Thread.sleep(1000);
        }

    }
}