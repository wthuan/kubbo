package com.ifeng.kubbo.remote.akka;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.ifeng.kubbo.remote.Ref;
import com.typesafe.config.ConfigFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.ifeng.kubbo.remote.akka.Constants.*;


public  class RefInbox implements Ref {


    private ConcurrentMap<String,ActorRef> routerMaps = new ConcurrentHashMap<>(1<<8);
    private ConcurrentMap<String,Object> proxyMaps = new ConcurrentHashMap<>(1<<8);

    private ActorSystem system;
    private TypedActorExtension typed;

    private LoggingAdapter logger;
    public static RefInbox get(String seedNodes,int port) {

        ActorSystem system = ActorSystem.create(SYSTEM, ConfigFactory.parseString("akka.cluster.roles=[" + CONSUMER_ROLE + "]")
                .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
                .withFallback(ConfigFactory.parseString("akka.cluster.seed-nodes=[\"" + seedNodes + "\"]")))
                .withFallback(ConfigFactory.load()));
        RefInbox inbox = new RefInbox();

        inbox.system = system;
        inbox.typed = TypedActor.get(system);
        inbox.logger = Logging.getLogger(system,inbox);
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

        T proxy = null;
        ActorRef router = routerMaps.get(path);

        if (router == null) {
            synchronized (path.intern()){
                if(!routerMaps.containsKey(path)) {
                    try {
                        logger.info("waiting for register|provider={}",path);
                        path.intern().wait(WAIT_PROVIDER_REGISTER_TIME);
                    } catch (InterruptedException e) {
                        //
                    }
                    router = routerMaps.get(path);
                    if(router !=null){
                        proxy = (T)typed.typedActorOf(new TypedProps<>(clazz), router);
                        proxyMaps.put(path, proxy);
                    }
                }
            }

        }
        proxy = (T)proxyMaps.get(path);
        if(proxy == null) {
            throw new IllegalArgumentException("Not found provider " + clazz.getCanonicalName());
        }
        return proxy;
    }



    //default
    Map<String,ActorRef> getRouterMap(){
        return this.routerMaps;
    }
}