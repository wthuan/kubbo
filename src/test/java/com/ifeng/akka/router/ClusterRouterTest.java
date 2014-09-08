package com.ifeng.akka.router;

import akka.actor.*;
import akka.cluster.routing.ClusterRouterGroup;
import akka.cluster.routing.ClusterRouterGroupSettings;
import akka.routing.RoundRobinGroup;
import com.google.common.collect.Lists;
import com.ifeng.TestService;
import com.ifeng.TestServiceImpl;
import com.typesafe.config.ConfigFactory;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.List;

import static com.ifeng.kubbo.remote.akka.Constants.PROVIDER_ROLE;

/**
 * <title>ClusterRouterTest</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-8
 */
public class ClusterRouterTest extends TestCase{


    public void startProvider(int port) throws InterruptedException {
        ActorSystem system = ActorSystem.create("kubbo", ConfigFactory.parseString("akka.cluster.roles=[" + PROVIDER_ROLE + "]")
                .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
                        .withFallback(ConfigFactory.load())));
        TypedActorExtension typed = TypedActor.get(system);
        Object proxy = typed.typedActorOf(new TypedProps<TestService>(TestService.class, () -> new TestServiceImpl()),"com.ifeng.TestService");
        System.out.println("start provider");
        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test
    public void testStartProvider1() throws InterruptedException {

        startProvider(1111);
    }
    @Test
    public void testStartProvider2() throws InterruptedException {
        startProvider(2222);
    }


    @Test
    public void testStartConsumer() throws InterruptedException {
        ActorSystem system = ActorSystem.create("kubbo", ConfigFactory.parseString("akka.cluster.roles=[" + "consumer" + "]")
                .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 5555)
                        .withFallback(ConfigFactory.load())));
        List<String> path = Lists.newArrayList("/user/com.ifeng.TestService");
        ActorRef router = system.actorOf(new ClusterRouterGroup(new RoundRobinGroup(path), new ClusterRouterGroupSettings(10, path, false, PROVIDER_ROLE)).props());
        TypedActorExtension typed = TypedActor.get(system);
        TestService testService = typed.typedActorOf(new TypedProps<TestService>(TestService.class),router);
//        Thread.sleep(3000);
        int i =0;
        for(;;){
            try{
                System.out.println(++i);
                testService.testReturnIntWithParam(i);

            }catch(Exception e){
                System.out.println(e);
            }

            Thread.sleep(1000);
        }

//        Thread.sleep(Integer.MAX_VALUE);
    }
}