package com.ifeng.kubbo.remote.akka;

import akka.actor.*;
import akka.cluster.routing.ClusterRouterGroup;
import akka.cluster.routing.ClusterRouterGroupSettings;
import akka.routing.RoundRobinGroup;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ifeng.kubbo.remote.Ref;
import com.typesafe.config.ConfigFactory;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static com.ifeng.kubbo.remote.akka.Constants.*;

/**
 * <title>Reference</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-9
 */
public class Reference implements Ref {

    private ConcurrentMap<ProviderConfig,Object> refMap = Maps.newConcurrentMap();
    private ActorSystem system;
    private TypedActorExtension typed;


    private Reference(ActorSystem system){
        this.system = system;
        this.typed = TypedActor.get(system);
    }

    private static Reference INSTANCE;



    public static Reference get(String seedNodes,int port){
        if(INSTANCE  == null) {
            ActorSystem system = ActorSystem.create(SYSTEM, ConfigFactory.parseString("akka.cluster.roles=[" + CONSUMER_ROLE + "]")
                    .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
                            .withFallback(ConfigFactory.parseString("akka.cluster.seed-nodes=[\"" + seedNodes + "\"]")))
                    .withFallback(ConfigFactory.load()));

            Reference reference = new Reference(system);
            INSTANCE = reference;
        }
        return INSTANCE;
    }



    @Override
    public <T> T getRef(Class<? super T> clazz, String group, String version) {
        ProviderConfig config = new ProviderConfig(clazz,group,version);
        Object ref = refMap.get(config);
        if(ref == null){
            String akkaPath = config.toAkkaPath().intern();
            synchronized (akkaPath){
                if(refMap.containsKey(config)){
                    return (T)refMap.get(config);
                }
                List<String> routees = Lists.newArrayList(akkaPath);

                ActorRef router = system.actorOf(new ClusterRouterGroup(
                        new RoundRobinGroup(routees),
                        new ClusterRouterGroupSettings(PROVIDER_TOTAL_INSTANCE,
                                routees, true, PROVIDER_ROLE)).props());
                Object proxy = typed.typedActorOf(new TypedProps<>(clazz),router);
                refMap.put(config,proxy);
            }
        }

        return (T)refMap.get(config);
    }
}