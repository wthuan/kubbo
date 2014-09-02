package com.ifeng.akka.router;

import akka.actor.*;
import akka.japi.Creator;
import akka.remote.routing.RemoteRouterConfig;
import akka.routing.RoundRobinGroup;
import akka.routing.RoundRobinPool;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;

import java.util.List;

/**
 * <title>RouterwithCheckDeath</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-2
 */
public class RouterWithCheckDeath {

    public static interface Test{
        public void test();
    }

    public static class TestImpl implements Test{

        private int id;
        public TestImpl(int id){
            this.id = id;
        }
        @Override
        public void test() {
            System.out.println(Thread.currentThread().getName()+" execute test ["+id+"]");
        }
        public static TypedProps typedProps(int id){
            return new TypedProps(Test.class,new Creator<Test>(){
                @Override
                public Test create() throws Exception {
                    return new TestImpl(id);
                }
            });
        }
    }


    public static Object startTypedActor(int port,String actorName){
        ActorSystem system = ActorSystem.create("kubbo", ConfigFactory.parseString("akka.remote.netty.tcp.port="+port).withFallback(
                ConfigFactory.load("cluster")
        ));
        TypedActorExtension typed = TypedActor.get(system);
        return typed.typedActorOf(TestImpl.typedProps(port),actorName);
    }
    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("kubbo");
        TypedActorExtension typed = TypedActor.get(system);
        Object proxy1 = startTypedActor(1111,"test1");
        System.out.println("start actor 1");
        Thread.sleep(3000);
        Object proxy2 = startTypedActor(2222,"test2");
        System.out.println("start actor 2");
        Thread.sleep(3000);
        Object proxy3 = startTypedActor(3333,"test3");
        System.out.println("start actor 3");

        List<String> list = Lists.newArrayList("akka.tcp://kubbo@127.0.0.1:1111/user/test1","akka.tcp://kubbo@127.0.0.1:2222/user/test2","akka.tcp://kubbo@127.0.0.1:3333/user/test3");
//        RoundRobinGroup roundRobinGroup = new RoundRobinGroup(list);
//        ActorRef router = system.actorOf(roundRobinGroup.props());
        system.actorOf(new RemoteRouterConfig(new RoundRobinPool(list.size()),new Address[]{new Address("akka.tcp","kubbo","127.0.0.1",1111)}).props())
        Test test = typed.typedActorOf(new TypedProps<Test>(Test.class),router);
        int i = 0;
        for(;;){
            i++;
            test.test();
            Thread.sleep(1000);
            if(i == 10){
                System.out.println("stop router");
                router.tell(PoisonPill.getInstance(),null);
            }
//            if(i == 10){
//                typed.stop(proxy1);
//            }
//            if(i == 20){
//                typed.stop(proxy2);
//            }
//            if(i == 30){
//                typed.stop(proxy3);
//            }
        }
    }
}