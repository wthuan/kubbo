package com.ifeng.kubbo.remote.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.ifeng.kubbo.remote.ProviderManager;
import com.typesafe.config.ConfigFactory;

import java.util.Set;

/**
 * <title>ProviderManagerInbox</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-1
 */
public class ProviderManagerInbox implements ProviderManager {

    private static final String SYSTEM = "kubbo";
    private static final String ROLE = "provider";
    private static final String CONSUMER = "/user/consumer";
    private ActorRef providerMgrActorRef;

    private ProviderManagerInbox(ActorRef providerMgrActorRef) {
        this.providerMgrActorRef = providerMgrActorRef;

    }

    public static ProviderManagerInbox get(Set<ProviderConfig> servicePublished, String seedNodes,int port) {

        ActorSystem system = ActorSystem.create(SYSTEM, ConfigFactory.parseString("akka.cluster.roles=[" + ROLE + "]").
                withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)).
                withFallback(ConfigFactory.parseString("akka.cluster.seed-nodes=[\"" + seedNodes + "\"]"))
                .withFallback(ConfigFactory.load()));
        ActorRef actorRef = system.actorOf(ProviderActor.props(servicePublished), "providerMgr");

        return new ProviderManagerInbox(actorRef);
    }


    @Override
    public void publish(Class<?> clazz, Object implement, String group, String version) {

    }

    @Override
    public void remove(Class<?> clazz, String group, String version) {

    }
}