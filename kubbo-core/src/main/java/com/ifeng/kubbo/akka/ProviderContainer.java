package com.ifeng.kubbo.akka;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.*;
import com.google.common.collect.Lists;
import com.ifeng.kubbo.ProviderLifeCycle;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.reflect.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

import static com.ifeng.kubbo.akka.Constants.PROVIDER_ROLE;
import static com.ifeng.kubbo.akka.Constants.TYPED_ACTOR_NUM;


public class ProviderContainer implements ProviderLifeCycle {



    private final ActorSystem system;

    private LoggingAdapter logger;

    private final TypedActorExtension typed;

    private final int typedActorNum;

    private final ConcurrentMap<ProviderConfig, ProviderMetadata> metadataMap = new ConcurrentHashMap<>();


    public ProviderContainer() {
        this(TYPED_ACTOR_NUM);
    }

    public ProviderContainer(int typedActorNum) {

        this.system = ActorSystem.create(Constants.SYSTEM,
                ConfigFactory.parseString("akka.cluster.roles=[" + PROVIDER_ROLE + "]")
                        .withFallback(ConfigFactory.load()));
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
    public <T> T start(Class<T> clazz, T implement, String group, String version) {

        //param check
        Objects.requireNonNull(clazz, "clazz required non null");
        Objects.requireNonNull(implement, "implement required non null");

        if (!TypeUtils.isInstance(group, clazz)) {
            throw new KubboException(implement + " must implements " + clazz);
        }

        ProviderConfig config = new ProviderConfig(clazz, implement, group, version);

        //getRef from cache
        ProviderMetadata metadata = metadataMap.get(config);
        if (metadata!=null) {
            if (logger.isInfoEnabled()) {
                logger.info("provider already start|path={}", config.toPath());
            }
            return (T) metadata.getRouterProxy();
        }

        synchronized (clazz){

            if(metadataMap.containsKey(config)) {
                return (T) metadataMap.get(config).getRouterProxy();
            }
            //actual start
            List<T> proxyList = new ArrayList<>(typedActorNum);
            List<ActorRef> actorRefList = new ArrayList<>(typedActorNum);
            List<String> routeePaths = new ArrayList<>(typedActorNum);

            for (int i = 0; i < typedActorNum; i++) {
                T proxy = typed.typedActorOf(new TypedProps<>(clazz, () -> implement));
                ActorRef actorRef = typed.getActorRefFor(proxy);
                proxyList.add(proxy);
                actorRefList.add(actorRef);
                routeePaths.add(actorRef.path().toStringWithoutAddress());
            }


            RoundRobinGroup roundRobinGroup = new RoundRobinGroup(routeePaths);
            //akka actor name is unique
            ActorRef router = system.actorOf(roundRobinGroup.props(), config.toPath());
            T routerProxy = typed.typedActorOf(new TypedProps<>(clazz), router);


            metadata = new ProviderMetadata<T>(config, router, routerProxy);

            metadataMap.put(config, metadata);


//            //tell listener to notify all consumer
//            providerListener.tell(Protocol.SHAKE_HAND,router);


            logger.info("start provider success|path={}", config);
            return routerProxy;
        }
    }



    @Override
    public void stop(Class<?> clazz, String group, String version) {
        ProviderConfig config = new ProviderConfig(clazz,group,version);
        ProviderMetadata metadata = check(config);

        metadata.getRouter().tell(new Broadcast(PoisonPill.getInstance()), null);
        logger.info("stop provider success|path={}", config.toPath());
        metadataMap.remove(config);
    }




    @Override
    public <T> T increaseActor(Class<T> clazz,String group,String version) {
        ProviderConfig config = new ProviderConfig(clazz, group, version);
        ProviderMetadata metadata = check(config);
        T newProxy = typed.typedActorOf(new TypedProps<>(clazz, () -> (T)metadata.getConfig().getImplement()));
        ActorRef newActorRef = typed.getActorRefFor(newProxy);

        //send add route msg
        Routee increseRoutee = new ActorRefRoutee(newActorRef);

        //send addRoutee msg to router
        metadata.getRouter().tell(new AddRoutee(increseRoutee), null);


        metadata.getIncresedRoutee().add(increseRoutee);

        logger.info("increase actor success|path={}", config.toPath());

        return (T)metadata.getRouterProxy();

    }

    @Override
    public <T> T decreaseActor(Class<T> clazz, String group, String version) {
        ProviderConfig config = new ProviderConfig(clazz, group, version);
        ProviderMetadata metadata = check(config);

        if (metadata.getIncresedRoutee().size() == 0) {
            logger.error("provider has no increased actor,not allowed decrease|path={}", config.toPath());
            return (T)metadata.getRouterProxy();
        }
        Routee removed = (Routee)metadata.getIncresedRoutee().remove(
                ThreadLocalRandom.current().nextInt(
                        metadata.getIncresedRoutee()
                                .size()));

        //router remove it,but it not stopped
        metadata.getRouter().tell(new RemoveRoutee(removed),null);

        //stop routee
        removed.send(PoisonPill.getInstance(),null);

        logger.info("decrease actor success|path={}",config.toPath());
        return (T) metadata.getRouterProxy();


    }





    private <T> ProviderMetadata check(ProviderConfig config) {
        ProviderMetadata metadata = metadataMap.get(config);
        if (metadata == null) {
            throw new KubboException("provider not found|clazz=" + config.getClassName() +
                    ",group=" + config.getGroup() +
                    ",version=" + config.getVersion());
        }
        return metadata;
    }


    public static class ProviderMetadata<T>{
        private final ProviderConfig config;

        private final ActorRef router;
        private final T routerProxy;
        private final List<Routee> incresedRoutee = Lists.newCopyOnWriteArrayList();
        public ProviderMetadata(ProviderConfig config,
                                ActorRef router,
                                T routerProxy){
            this.config = config;
            this.router = router;
            this.routerProxy = routerProxy;
        }


        public ProviderConfig getConfig() {
            return config;
        }


        public ActorRef getRouter() {
            return router;
        }

        public T getRouterProxy() {
            return routerProxy;
        }

        public List<Routee> getIncresedRoutee() {
            return incresedRoutee;
        }
    }

}