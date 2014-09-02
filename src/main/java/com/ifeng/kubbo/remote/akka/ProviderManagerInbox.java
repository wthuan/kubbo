
package com.ifeng.kubbo.remote.akka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import com.google.common.collect.Sets;
import com.ifeng.kubbo.remote.ProviderManager;
import com.typesafe.config.ConfigFactory;

import java.util.Set;

import static com.ifeng.kubbo.remote.akka.Constants.PROVIDER_ACTOR;
import static com.ifeng.kubbo.remote.akka.Constants.PROVIDER_ROLE;
import static com.ifeng.kubbo.remote.akka.Constants.SYSTEM;

/**
 * <title>ProviderManagerInbox</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-1
 */
public class ProviderManagerInbox implements ProviderManager {




    private ActorRef providerMgrActorRef;
    private Inbox inbox;
    private ProviderManagerInbox(ActorSystem system,Set<ProviderConfig> servicePublished) {
        this.providerMgrActorRef = system.actorOf(ProviderActor.props(servicePublished), PROVIDER_ACTOR);
        inbox = Inbox.create(system);
    }

    public static ProviderManagerInbox get(Set<ProviderConfig> servicePublished, String seedNodes,int port) {

        ActorSystem system = ActorSystem.create(SYSTEM, ConfigFactory.parseString("akka.cluster.roles=[" + PROVIDER_ROLE + "]").
                withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)).
                withFallback(ConfigFactory.parseString("akka.cluster.seed-nodes=[\"" + seedNodes + "\"]"))
                .withFallback(ConfigFactory.load()));
        return new ProviderManagerInbox(system,servicePublished);
    }


    @Override
    public void publish(Class<?> clazz, Object implement, String group, String version) {
        final ProviderConfig config = new ProviderConfig(clazz,implement,group,version);
        Protocol.Register registerMsg = new Protocol.Register(Sets.newHashSet(config));
        inbox.send(providerMgrActorRef,registerMsg);
    }

    @Override
    public void remove(Class<?> clazz, String group, String version) {
        final ProviderConfig config = new ProviderConfig(clazz,group,version);
        Protocol.UnRegister unRegisterMsg = new Protocol.UnRegister(Sets.newHashSet(config));
        inbox.send(providerMgrActorRef,unRegisterMsg);
    }
}
