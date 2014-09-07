package com.ifeng.kubbo.remote.akka;

import akka.actor.*;
import com.ifeng.kubbo.remote.Reference;
import com.typesafe.config.ConfigFactory;

import java.util.HashMap;
import java.util.Map;

import static com.ifeng.kubbo.remote.akka.Constants.CONSUMER_ROLE;
import static com.ifeng.kubbo.remote.akka.Constants.SYSTEM;


public  class ReferenceInbox implements Reference {


    private Map<String,ActorRef> routerMaps = new HashMap<>();
    private Map<String,Object> proxyMaps = new HashMap<>();
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
        system.actorOf(ReferenceListenerActor.props(inbox), Constants.CONSUMER_ACTOR);
        return inbox;
    }

    @Override
    public <T> T getRef(Class<? super T> clazz, String group, String version) {
        ProviderConfig config = new ProviderConfig(clazz, null, group, version);
        String path = "/user/"+config.toPath();
        if(proxyMaps.containsKey(path)){
            return (T)proxyMaps.get(path);
        }
        ActorRef router = routerMaps.get(path);
        if (router == null) {
            throw new IllegalArgumentException("Not found provider " + clazz.getCanonicalName());
        }
        synchronized (this){
            Object proxy = typed.typedActorOf(new TypedProps<>(clazz), router);
            proxyMaps.put(path, proxy);
        }
        return (T)proxyMaps.get(path);
    }


    public void refresh(Map<String, ActorRef> routerMap) {
        this.routerMaps = routerMap;
    }

}