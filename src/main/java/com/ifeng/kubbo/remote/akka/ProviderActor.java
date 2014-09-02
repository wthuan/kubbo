package com.ifeng.kubbo.remote.akka;

import akka.actor.*;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.contrib.pattern.DistributedPubSubExtension;
import akka.contrib.pattern.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.ifeng.kubbo.remote.akka.Constants.CONSUMER_ACTOR_PATH;
import static com.ifeng.kubbo.remote.akka.Constants.CONSUMER_ROLE;

/**
 * <title>AkkaServicePushlisher</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-1
 */
public class ProviderActor extends UntypedActor{

    private Cluster cluster = Cluster.get(getContext().system());
    private ActorRef mediator = DistributedPubSubExtension.get(getContext().system()).mediator();
    private LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private TypedActorExtension typed = TypedActor.get(getContext().system());
    private Set<ProviderConfig> providerPublished;
    private Map<ProviderConfig,Object> proxyMap = new HashMap<>();


    private ProviderActor(Set<ProviderConfig> providerPublished) {
        this.providerPublished = providerPublished;
    }


    @Override
    public void preStart() throws Exception {
        if (providerPublished != null && !providerPublished.isEmpty()) {
            providerPublished.forEach(this::registerProvider);
        }

        cluster.subscribe(getSelf(), ClusterEvent.MemberUp.class,
                ClusterEvent.MemberRemoved.class);
    }

    @Override
    public void postStop() throws Exception {
        cluster.unsubscribe(getSelf());
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if(message instanceof ClusterEvent.CurrentClusterState) {
            ClusterEvent.CurrentClusterState clusterState = (ClusterEvent.CurrentClusterState) message;
            for (Member member : clusterState.getMembers()) {
                if (isAliveConsumer(member)) {
                    registerConsumer(member);
                }
            }
        }else if (message instanceof ClusterEvent.MemberUp) {
            ClusterEvent.MemberUp memberUp = (ClusterEvent.MemberUp) message;
            if (isAliveConsumer(memberUp.member())) {
                registerConsumer(memberUp.member());
            }
        }else if(message instanceof Protocol.Register) {
            Protocol.Register registerMsg = (Protocol.Register) message;
            publishProvider(registerMsg.getProviderConfigs());
        }else if(message instanceof Protocol.UnRegister){
            Protocol.UnRegister unRegisterMsg = (Protocol.UnRegister) message;
            removeProvider(unRegisterMsg.getProviderConfigs());
        }else{
            logger.warning("UnKnown message {}",message);
            unhandled(message);
        }
    }

    private void registerConsumer(Member member) {
        logger.info("sender register consumer command to {}", member.address());
        getContext().actorSelection(member.address() + CONSUMER_ACTOR_PATH).tell(new Protocol.Register(providerPublished), getSelf());
    }


    public void publishProvider(Set<ProviderConfig> providerConfigs) {
        providerConfigs.removeAll(providerPublished);

//
//        if (providerPublished.contains(providerConfig)) {
//            logger.warning("provider already published|provider={}", providerConfig.toPath());
//            return;
//        }

        if (!providerConfigs.isEmpty()) {
            providerConfigs.forEach(p-> {
                if (!registerProvider(p)) {
                    providerConfigs.remove(p);
                }
            });
            if (providerConfigs.size() > 0) {
                mediator.tell(new DistributedPubSubMediator.SendToAll(CONSUMER_ACTOR_PATH, new Protocol.Register(providerConfigs)), getSelf());
            }
            logger.info("sendToAll consumer newProvider {}",providerConfigs);
        }

    }


    public void removeProvider(Set<ProviderConfig> providerConfigs){
        providerConfigs.retainAll(providerPublished);
        if(logger.isDebugEnabled()){
            logger.debug("remove providers {}",providerConfigs);
        }

        if(!providerConfigs.isEmpty()){
            providerPublished.removeAll(providerConfigs);
            for(ProviderConfig providerConfig : providerConfigs){
                Object proxy = proxyMap.get(providerConfig);
                if(proxy == null){
                    logger.warning("provider proxy is missing|provider={}",providerConfig);
                    break;
                }
                if(typed.stop(proxy)){
                    logger.info("provider is stopped|provider={}",providerConfig);
                }else{
                    logger.warning("provider stopped failure|provider={}", providerConfig);
                }

            }
            mediator.tell(new DistributedPubSubMediator.SendToAll(CONSUMER_ACTOR_PATH,new Protocol.UnRegister(providerConfigs)),getSelf());
        }
    }

    private boolean isAliveConsumer(Member member) {
        return member.hasRole(CONSUMER_ROLE) && (member.status() == MemberStatus.up());
    }


    private boolean registerProvider(ProviderConfig providerConfig) {
        try {
            Object proxy = typed.typedActorOf(new TypedProps(providerConfig.getClazz(), () -> providerConfig.getImplement()), providerConfig.toPath());
            proxyMap.put(providerConfig,proxy);
            logger.info("register provider {} success", proxy);
            return true;
        } catch (Exception e) {
            logger.error("register provider {} error:{}",providerConfig.toPath(),e.getMessage());
        }
        return false;

    }
    public static Props props(Set<ProviderConfig>servicePublished) {
        return Props.create(ProviderActor.class, servicePublished);
    }
}
