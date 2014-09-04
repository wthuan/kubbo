package com.ifeng.kubbo.remote.akka;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.*;
import com.ifeng.kubbo.remote.ProviderFactory;
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
public class TypedActorProviderFactory implements ProviderFactory {



    private final ActorSystem system;

    private LoggingAdapter logger;

    private final TypedActorExtension typed;

    private final int typedActorNum;

    private final ConcurrentMap<ProviderConfig, ProviderMetadata> metadatas = new ConcurrentHashMap<>();




    public TypedActorProviderFactory(ActorSystem system, int typedActorNum) {
        Objects.requireNonNull(system, "actorSystem required non null");

        this.system = system;
        this.typed = TypedActor.get(system);
        this.typedActorNum = typedActorNum > 0 ? typedActorNum : TYPED_ACTOR_NUM;
        this.logger = Logging.getLogger(system, this);
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
    public <T> T create(Class<T> clazz, T implement, String group, String version) {

        //param check
        Objects.requireNonNull(clazz, "clazz required non null");
        Objects.requireNonNull(implement, "implement required non null");

        if (!TypeUtils.isInstance(group, clazz)) {
            throw new KubboException(implement + " must implements " + clazz);
        }

        ProviderConfig config = new ProviderConfig(clazz, implement, group, version);

        //get from cache
        ProviderMetadata metadata = metadatas.get(config);
        if (metadatas.containsKey(config)) {
            if (logger.isInfoEnabled()) {
                logger.info("provider already create|path={}", config.toPath());
            }
            return (T) metadata.getRouterProxy();
        }


        //actual create
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
        logger.info("create provider success|path={}", config);
        metadata = new ProviderMetadata<>(config, proxyList, actorRefList, router, routerProxy);
        metadatas.putIfAbsent(config, metadata);
        return routerProxy;
    }


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



    public <T> T decreaseActor(Class<?> clazz, String group, String version) {
        ProviderConfig config = new ProviderConfig(clazz, null, group, version);
        ProviderMetadata metadata = check(config);
        if (metadata.getIncresedActors().size() == 0) {
            logger.error("provider has no increased actor,not allowed decrease|path={}", config.toPath());
            return null;
        }
        Routee removed = (Routee)metadata.getIncresedActors().remove(ThreadLocalRandom.current().nextInt(metadata.getIncresedActors().size()));
        metadata.getRouter().tell(new RemoveRoutee(removed),null);
        return (T) metadata.getRouterProxy();


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