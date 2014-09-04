package com.ifeng.kubbo.remote.akka;

import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedActorExtension;
import akka.routing.RoundRobinGroup;
import com.typesafe.config.ConfigFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.ifeng.kubbo.remote.akka.Constants.CONSUMER_ROLE;
import static com.ifeng.kubbo.remote.akka.Constants.SYSTEM;

/**
 * <title>ReferenceInbox</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-2
 */
public  class ReferenceInbox {


    private volatile ConcurrentMap<String, Set<String>> pathMap = new ConcurrentHashMap<>();
    private ReferenceInbox(){}

    private ActorSystem system;
    private TypedActorExtension typed;
    public static ReferenceInbox get(String seedNodes,int port) {
        ActorSystem system = ActorSystem.create(SYSTEM, ConfigFactory.parseString("akka.cluster.roles=[" + CONSUMER_ROLE + "]")
                .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
                        .withFallback(ConfigFactory.parseString("akka.cluster.seed-nodes=[\"" + seedNodes + "\"]")))
                .withFallback(ConfigFactory.load()));
        ReferenceInbox inbox = new ReferenceInbox();
        inbox.system = system;
        inbox.typed = TypedActor.get(system);
        system.actorOf(ReferenceActor.props(inbox), Constants.CONSUMER_ACTOR);
        return inbox;
    }

    public <T> T getReference(Class<? extends T> clazz, String group, String version) {
        Set<String> path = pathMap.get(clazz.getCanonicalName());
        if (path == null || path.size() == 0) {
            throw new IllegalArgumentException("Not found provider " + clazz.getCanonicalName());
        }
        RoundRobinGroup roundRobinGroup = new RoundRobinGroup(path);
//        ActorRef router = system.actorOf(roundRobinGroup.props());
//        Router router = roundRobinGroup.createRouter(system);
//        router.removeRoutee(new ActorSelectionRoutee(system.actorSelection(path.)))
//        return (T) typed.typedActorOf(new TypedProps<>(clazz), router);
        return null;
    }


    public void refreshPaths(ConcurrentMap<String, Set<String>> pathMap) {
        this.pathMap = pathMap;
    }




}