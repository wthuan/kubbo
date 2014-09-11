package com.ifeng.akka;

import akka.actor.*;
import akka.dispatch.OnComplete;
import akka.routing.Broadcast;
import akka.routing.RoundRobinGroup;
import com.google.common.collect.Lists;
import com.ifeng.TestService;
import com.ifeng.TestServiceImpl;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Future;

/**
 * <title>TypedActorStopTest</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-5
 */
public class TypedActorStopTest {


    public static void stopTypedActorDirectly() throws Exception {
        ActorSystem system = ActorSystem.create("kubbo", ConfigFactory.parseString("akka.remote.netty.tcp.port=1111"));
        TypedActorExtension typed = TypedActor.get(system);
        TestService service = typed.typedActorOf(new TypedProps<TestService>(TestService.class, () -> new TestServiceImpl()),"testservice");
        service.testVoid(1000);
        System.out.println("start stop typedActor");
        typed.getActorRefFor(service).tell(PoisonPill.getInstance(),null);
        Future future = service.testReturnFuture(1000);
        future.onComplete(new OnComplete() {
            @Override
            public void onComplete(Throwable failure, Object success) throws Throwable {
                System.out.println(failure);
            }
        },system.dispatcher());
    }


    public static void stopRouter() throws Exception {
        ActorSystem system = ActorSystem.create("kubbo", ConfigFactory.empty());
        TypedActorExtension typed = TypedActor.get(system);
        TestService service = typed.typedActorOf(new TypedProps<TestService>(TestService.class, () -> new TestServiceImpl()),"testservice");
        ActorRef actorRef = typed.getActorRefFor(service);
        RoundRobinGroup group = new RoundRobinGroup(Lists.newArrayList(actorRef.path().toStringWithoutAddress()));
        ActorRef router = system.actorOf(group.props(), "router");

        TestService serviceRouter = typed.typedActorOf(new TypedProps<TestService>(TestService.class), router);

        router.tell(new Broadcast(PoisonPill.getInstance()), null);

        Thread.sleep(1000);
        router.tell("hello", null);

        service.testVoid(1000);
    }
    public static void main(String[] args) throws Exception {
        stopRouter();
    }
}