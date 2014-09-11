package com.ifeng.akka.router;

import akka.actor.*;
import akka.routing.*;
import com.google.common.collect.Lists;
import com.ifeng.TestService;
import com.ifeng.TestServiceImpl;
import com.typesafe.config.ConfigFactory;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <title>RouteeRemoveTest</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-7
 */
public class RouteeRemoveTest extends TestCase {

    public static class MyActor extends UntypedActor {
        private static final AtomicInteger counter = new AtomicInteger();
        private int id = counter.getAndIncrement();

        @Override
        public void onReceive(Object message) throws Exception {
            System.out.println(message+"///"+id);
        }


        @Override
        public void postStop() throws Exception {
            super.postStop();
            System.out.println("stop");
        }
    }

    @Test
    public void testRemoveRouteeIfStop() throws InterruptedException {
        ActorSystem system = ActorSystem.create("kubbo", ConfigFactory.empty());
        ActorRef actorRef1 = system.actorOf(Props.create(MyActor.class), "test1");
        ActorRef actorRef2 = system.actorOf(Props.create(MyActor.class), "test2");
        ActorRef actorRef3 = system.actorOf(Props.create(MyActor.class), "test3");
        ArrayList<String> list1 = Lists.newArrayList(actorRef1.path().toStringWithoutAddress(), actorRef2.path().toStringWithoutAddress());
        RoundRobinGroup group = new RoundRobinGroup(list1);
//        Router router = group.createRouter(system);
//
        ActorRef router = system.actorOf(group.props());
//        router.tell("hell0",null);

        Routee routee =new ActorRefRoutee(actorRef3);
        router.tell(new AddRoutee(routee),null);

        int i= 0;
        for(;;){
            router.tell("hello",null);

            Thread.sleep(1000);
            if(++i == 5){
//                router.tell(new RemoveRoutee(routee),null);
                routee.send(PoisonPill.getInstance(),null);
//                actorRef3.tell(PoisonPill.getInstance(),null);
            }
        }
    }


    @Test
    public void testAddRoute() throws InterruptedException {
        ActorSystem system = ActorSystem.create("kubbo", ConfigFactory.empty());
        Inbox inbox = Inbox.create(system);
        ActorRef sender = inbox.getRef();
        ActorRef worker1 = system.actorOf(Props.create(MyActor.class),"worker1");
        ActorRef worker2 = system.actorOf(Props.create(MyActor.class),"worker2");
        RoundRobinGroup group = new RoundRobinGroup(Lists.newArrayList());
        ActorRef router = system.actorOf(group.props(),"router");
        ActorRefRoutee routee1 = new ActorRefRoutee(worker1);
        ActorRefRoutee routee2 = new ActorRefRoutee(worker2);
        router.tell(new AddRoutee(routee1),sender);
        router.tell(new AddRoutee(routee2),sender);

//        router.tell(new RemoveRoutee(routee),null);
        Thread.sleep(1000);
        int i = 0;
        for(;;) {
            router.tell("hello", sender);
            if(++i == 6){
                system.stop(worker1);
                Thread.sleep(1000);
                router.tell(new RemoveRoutee(routee1),sender);

            }
            Thread.sleep(1000);
        }

    }


    @Test
    public void testAddTypeRoute()throws Exception{
        ActorSystem system = ActorSystem.create("kubbo",ConfigFactory.empty());
        TypedActorExtension typed = TypedActor.get(system);
        Object proxy1 = typed.typedActorOf(new TypedProps<TestService>(TestService.class, () -> new TestServiceImpl()),"s1");
        Object proxy2 = typed.typedActorOf(new TypedProps<TestService>(TestService.class, () -> new TestServiceImpl()),"s2");
        ActorRef actorRef1 = typed.getActorRefFor(proxy1);
        ActorRef actorRef2 = typed.getActorRefFor(proxy2);
        RoundRobinGroup group = new RoundRobinGroup(Lists.newArrayList());
        ActorRef router = system.actorOf(group.props(),"router");
        TestService service = typed.typedActorOf(new TypedProps<TestService>(TestService.class),router);
        ActorRefRoutee routee1 = new ActorRefRoutee(actorRef1);
        ActorRefRoutee routee2 = new ActorRefRoutee(actorRef2);
        router.tell(new AddRoutee(routee1),null);
        Thread.sleep(3000);

        int i= 0;
        for(;;){
            i++;
            try{
                service.testReturnIntWithParam(1);
            }catch(Exception e){
                System.out.println(e.toString());
            }

            if(i == 5){
                router.tell(new AddRoutee(routee2),null);
                System.out.println("add");
            }

            if(i == 10){
                typed.stop(proxy2);
                System.out.println("stop proxy2");
                Thread.sleep(3000);
                router.tell(new RemoveRoutee(routee2),null);

            }
            Thread.sleep(1000);
        }
    }
}