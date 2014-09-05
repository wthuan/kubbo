package com.ifeng.akka;

import akka.actor.*;
import akka.dispatch.Futures;
import akka.routing.*;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <title>ActorThreadTest</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-3
 */
public class ActorThreadTest {

    public static final long alive = 100;
    public static interface  Test{
        public Future test() throws InterruptedException;
    }

    public static class TestImpl implements Test{

        private int id;

        public TestImpl(int id) {
            this.id = id;
        }
        int seq = 0;
        @Override
        public Future test() throws InterruptedException {

            System.out.println(Thread.currentThread().getName() + "/"+id+""+" execute cost " + alive);
            return Futures.successful(1);
        }

        public static TypedProps typedProps(int id){
            return new TypedProps(Test.class,()->new TestImpl(id));
        }

    }

    public static class MyUntypedActor extends UntypedActor {

        private int id;

        public MyUntypedActor(int id) {
            this.id = id;
        }


        @Override
        public void onReceive(Object message) throws Exception {
            System.out.println(Thread.currentThread().getName() + "/"+id+""+" execute cost " + alive);
            Thread.sleep(alive);
        }
        @Override
        public void postStop(){
            System.out.println("untypedActor stopped");
        }

        public static Props props(int id) {
            return Props.create(MyUntypedActor.class, () -> new MyUntypedActor(id));
        }
    }

    public static class Watcher extends UntypedActor {
        private Router router;
        @Override
        public void preStart() throws Exception {
            ActorRef actorRef = getContext().actorOf(MyUntypedActor.props(1));
            router = new Router(new RoundRobinRoutingLogic());
            router.addRoutee(new ActorRefRoutee(actorRef));
            getContext().watch(actorRef);
            getContext().system().scheduler().scheduleOnce(FiniteDuration.create(5,TimeUnit.SECONDS),new Runnable(){
                @Override
                public void run() {
                    actorRef.tell(PoisonPill.getInstance(), null);
                }
            },getContext().dispatcher());
        }

        @Override
        public void onReceive(Object message) throws Exception {
            System.out.println(message);
            if (message instanceof Terminated) {
                System.out.println("worker terminal");
                Terminated terminaledMsg = (Terminated) message;
                router.removeRoutee(new ActorRefRoutee(terminaledMsg.actor()));
            }

            router.route(message, getSelf());
        }
    }

    public static Object[] typedActor(int port, String actorName) {
        ActorSystem system = ActorSystem.create("kubbo", ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
                .withFallback(ConfigFactory.load()));
        TypedActorExtension typed = TypedActor.get(system);
        Object proxy1 = typed.typedActorOf(TestImpl.typedProps(port), actorName);
        Object proxy2 = typed.typedActorOf(TestImpl.typedProps(port + 1), actorName + "_1");
        Object proxy3 = typed.typedActorOf(TestImpl.typedProps(port + 2), actorName + "_2");
        ActorRef ref1 = typed.getActorRefFor(proxy1);
        ActorRef ref2 = typed.getActorRefFor(proxy2);
        ActorRef ref3 = typed.getActorRefFor(proxy3);
        List<String> path = Lists.newArrayList(ref1.path().toStringWithoutAddress(), ref2.path().toStringWithoutAddress(), ref3.path().toStringWithoutAddress());

//        Router router = new Router(new RoundRobinRoutingLogic(), Lists.newArrayList(new ActorRefRoutee(ref1), new ActorRefRoutee(ref2), new ActorRefRoutee(ref3)));
//
//        RoundRobinPool roundRobinPool = new RoundRobinPool(3);
        ActorRef router = system.actorOf(new RoundRobinGroup(path).props());
        Object proxy = typed.typedActorOf(new TypedProps<Test>(Test.class), router);
        return new Object[]{proxy,router};
    }

    public static ActorRef untypedActor(int port, String actorName) {
        ActorSystem system = ActorSystem.create("kubbo", ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
        .withFallback(ConfigFactory.load()));
        ActorRef actorRef = system.actorOf(MyUntypedActor.props(port), actorName);
        System.out.println("start untypedActor:" + actorRef);
        return actorRef;
    }


    public static void testUntypedActorThread(){
        ActorRef actorRef = untypedActor(1111, "untypedActor1");
        ActorSystem system = ActorSystem.create("kubbo");
        Inbox inbox = Inbox.create(system);
        for (int i = 0; i < 10000; i++) {

            inbox.send(actorRef, "hello");
        }
        System.out.println("send complemete");
    }

    public static void testRouter() throws InterruptedException {
        ActorRef actorRef1 = untypedActor(1111, "test1");
        ActorRef actorRef2 = untypedActor(2222, "test2");
        ActorRef actorRef3 = untypedActor(3333, "test3");
        List<Routee> routees = new ArrayList<>();
        routees.add(new ActorRefRoutee(actorRef1));
        routees.add(new ActorRefRoutee(actorRef2));
        routees.add(new ActorRefRoutee(actorRef3));
        Router router = new Router(new RoundRobinRoutingLogic(), routees);
        int i = 0;
        for (; ; ) {
            router.route("hello", null);
            if (++i == 5) {
                System.out.println("actorref1 stopped");
                actorRef1.tell(PoisonPill.getInstance(), null);
            }
            Thread.sleep(1000);
        }
    }


    public static void testWatcher() throws InterruptedException {
        ActorSystem system = ActorSystem.create("kubbo");
        ActorRef actorRef = system.actorOf(Props.create(Watcher.class, Watcher::new));
        int i=0;
        for(;;) {
            actorRef.tell("hello", null);
            Thread.sleep(1000);
        }
    }

    public  static void routeTypedActor() throws InterruptedException {
        Object[] ret =  typedActor(1111, "test1");
        ActorSystem system = ActorSystem.create("kubbo");
//        TypedActor.getRef(system).stop(test);
        Test test = (Test) ret[0];
        ActorRef router = ((ActorRef) ret[1]);
        int i=0;
        for (; ; ) {
            test.test();
            if (++i == 5) {
                Object proxy = TypedActor.get(system).typedActorOf(TestImpl.typedProps(4));
                ActorRef actorRefFor = TypedActor.get(system).getActorRefFor(proxy);
                router.tell(new AddRoutee(new ActorRefRoutee(actorRefFor)), null);
                System.out.println("add an typedActor");
            }
            Thread.sleep(1000);
        }
    }
    public static void main(String[] args) throws InterruptedException {
        routeTypedActor();

    }


}