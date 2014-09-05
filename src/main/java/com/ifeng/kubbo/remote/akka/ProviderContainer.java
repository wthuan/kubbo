package com.ifeng.kubbo.remote.akka;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.*;
import com.google.common.collect.Lists;
import com.ifeng.kubbo.remote.ProviderLifeCycle;
import org.apache.commons.lang3.reflect.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

import static com.ifeng.kubbo.remote.akka.Constants.TYPED_ACTOR_NUM;

/**
 * <title>TypedActorProviderCreator</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-4
 */
public class ProviderContainer implements ProviderLifeCycle {



    private final ActorSystem system;

    private LoggingAdapter logger;

    private final TypedActorExtension typed;

    private final int typedActorNum;

    private final ConcurrentMap<ProviderConfig, ProviderMetadata> metadatas = new ConcurrentHashMap<>();

    private ActorRef providerListener;

    public ProviderContainer(ActorSystem system, int typedActorNum) {
        Objects.requireNonNull(system, "actorSystem required non null");

        this.system = system;
        this.typed = TypedActor.get(system);
        this.typedActorNum = typedActorNum > 0 ? typedActorNum : TYPED_ACTOR_NUM;
        this.logger = Logging.getLogger(system, this);

        this.providerListener = system.actorOf(ProviderListenerActor.props(this));
    }


    /**
     *
     * @param clazz
     * @param implement
     * @param group
     * @param version
     * @param <T>
     * @return
     */
    @Override
    public <T> T start(Class<T> clazz, T implement, String group, String version) {

        //param check
        Objects.requireNonNull(clazz, "clazz required non null");
        Objects.requireNonNull(implement, "implement required non null");

        if (!TypeUtils.isInstance(group, clazz)) {
            throw new KubboException(implement + " must implements " + clazz);
        }

        ProviderConfig config = new ProviderConfig(clazz, implement, group, version);

        //getRef from cache
        ProviderMetadata metadata = metadatas.get(config);
        if (metadatas.containsKey(config)) {
            if (logger.isInfoEnabled()) {
                logger.info("provider already start|path={}", config.toPath());
            }
            return (T) metadata.getRouterProxy();
        }


        //actual start
        List<T> proxyList = new ArrayList<>(typedActorNum);
        List<ActorRef> actorRefList = new ArrayList<>(typedActorNum);
        List<String> routeePaths = new ArrayList<>(typedActorNum);
        List<Routee> routeeList = new ArrayList<>();
        for (int i = 0; i < typedActorNum; i++) {
            T proxy = typed.typedActorOf(new TypedProps<>(clazz, () -> implement));
            ActorRef actorRef = typed.getActorRefFor(proxy);
            proxyList.add(proxy);
            actorRefList.add(actorRef);
            routeePaths.add(actorRef.path().toStringWithoutAddress());
        }
        RoundRobinGroup roundRobinGroup = new RoundRobinGroup(routeePaths);
        ActorRef router = system.actorOf(roundRobinGroup.props(), config.toPath());
        T routerProxy = typed.typedActorOf(new TypedProps<>(clazz), router);
        metadata = new ProviderMetadata<>(config, proxyList, actorRefList, router, routerProxy);
        metadatas.putIfAbsent(config, metadata);
        providerListener.tell(Protocol.SHAKE_HANDS_ALL,router);
        logger.info("start provider success|path={}", config);
        return routerProxy;
    }

    @Override
    public void stop(Class<?> clazz, String group, String version) {
        ProviderConfig config = new ProviderConfig(clazz,null,group,version);
        ProviderMetadata metadata = check(config);

        //stop router
        system.stop(metadata.getRouter());
        typed.stop(metadata.getRouterProxy());

        //stop actoref
        List<ActorRef> refs = metadata.getActorRefList();
        refs.forEach(r->{r.tell(PoisonPill.getInstance(),null);});

        //stop proxy
        List<Object> proxys = metadata.getProxyList();
        proxys.forEach(typed::stop);

        //stop increased
        List<Routee> routees = metadata.getIncresedActors();
        for(Routee r : routees){
            r.send(PoisonPill.getInstance(),null);
        }
    }


    @Override
    public <T> T increaseActor(Class<T> clazz,String group,String version) {
        ProviderConfig config = new ProviderConfig(clazz, null, group, version);
        ProviderMetadata metadata = check(config);
        T newProxy = typed.typedActorOf(new TypedProps<>(clazz, () -> (T)metadata.getConfig().getImplement()));
        ActorRef newActorRef = typed.getActorRefFor(newProxy);

        //send add route msg
        Routee incresed = new ActorRefRoutee(newActorRef);
        metadata.getRouter().tell(new AddRoutee(incresed), null);
        metadata.getProxyList().add(newProxy);
        metadata.getActorRefList().add(newActorRef);
        metadata.getIncresedActors().add(incresed);
        logger.info("increase actor success|path={}", config.toPath());
        return (T)metadata.getRouterProxy();

    }

    @Override
    public <T> T decreaseActor(Class<T> clazz, String group, String version) {
        ProviderConfig config = new ProviderConfig(clazz, null, group, version);
        ProviderMetadata metadata = check(config);
        if (metadata.getIncresedActors().size() == 0) {
            logger.error("provider has no increased actor,not allowed decrease|path={}", config.toPath());
            return null;
        }
        Routee removed = (Routee)metadata.getIncresedActors().remove(ThreadLocalRandom.current().nextInt(metadata.getIncresedActors().size()));
        metadata.getRouter().tell(new RemoveRoutee(removed),null);
        logger.info("decrease actor success|path={}",config.toPath());
        return (T) metadata.getRouterProxy();


    }

    @Override
    public List<ActorRef> list() {
        List<ActorRef> refs = Lists.newArrayList();
        for (ProviderMetadata metadata : metadatas.values()) {
            refs.add(metadata.getRouter());
        }

        return refs;
    }


    private <T> ProviderMetadata check(ProviderConfig config) {
        ProviderMetadata metadata = metadatas.get(config);
        if (metadata == null) {
            throw new KubboException("provider not found|clazz=" + config.getKlass() +
                    ",group=" + config.getGroup() +
                    ",version=" + config.getVersion());
        }
        return metadata;
    }


    public static class ProviderMetadata<T>{
        private final ProviderConfig config;
        private final List<T> proxyList;
        private final List<ActorRef> actorRefList;

        private final ActorRef router;
        private final T routerProxy;
        private final List<Routee> incresedActors = new ArrayList<>();
        public ProviderMetadata(ProviderConfig config,
                                List<T> proxyList,
                                List<ActorRef> actorRefList,
                                ActorRef router,
                                T routerProxy){
            this.config = config;
            this.proxyList = proxyList;
            this.actorRefList = actorRefList;
            this.router = router;
            this.routerProxy = routerProxy;
        }


        public ProviderConfig getConfig() {
            return config;
        }

        public List<T> getProxyList() {
            return proxyList;
        }

        public List<ActorRef> getActorRefList() {
            return actorRefList;
        }

        public ActorRef getRouter() {
            return router;
        }

        public T getRouterProxy() {
            return routerProxy;
        }

        public List<Routee> getIncresedActors() {
            return incresedActors;
        }
    }

}