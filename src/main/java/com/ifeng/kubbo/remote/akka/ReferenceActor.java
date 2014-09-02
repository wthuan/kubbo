package com.ifeng.kubbo.remote.akka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.contrib.pattern.DistributedPubSubExtension;
import akka.contrib.pattern.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.collect.Sets;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <title>ReferenceActor</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-2
 */
public class ReferenceActor extends UntypedActor {

    private ActorRef mediator = DistributedPubSubExtension.get(getContext().system()).mediator();
    private Cluster cluster = Cluster.get(getContext().system());
    private LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    //服务-地址
    private ConcurrentMap<String, Set<String>> pathMap = new ConcurrentHashMap<>();
    //地址-服务
    private ConcurrentHashMap<String, Set<String>> hostMap = new ConcurrentHashMap<>();


    private ReferenceInbox referenceInbox;

    public ReferenceActor(ReferenceInbox referenceInbox) {
        this.referenceInbox = referenceInbox;
    }


    @Override
    public void preStart() throws Exception {
        mediator.tell(new DistributedPubSubMediator.Put(getSelf()), getSelf());
        cluster.subscribe(getSelf(), ClusterEvent.MemberUp.class,
                ClusterEvent.MemberRemoved.class);
    }

    @Override
    public void postStop() throws Exception {
        cluster.unsubscribe(getSelf());
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof Protocol.Register) {
            Protocol.Register registerMsg = (Protocol.Register) message;
            handleRegisterMsg(registerMsg,getSender());
        }else if (message instanceof Protocol.UnRegister) {
            Protocol.UnRegister unRegisterMsg = (Protocol.UnRegister) message;
        }else{
            logger.error("Unknown message {}", message);
        }
    }

    private void handleRegisterMsg(Protocol.Register registerMsg,ActorRef sender) {
        Set<ProviderConfig> providerConfigs = registerMsg.getProviderConfigs();
        String providerAddress = sender.path().address().toString();
        logger.info("handler register message|msg={}", providerConfigs);
        for (ProviderConfig pc : providerConfigs) {
            Set<String> providers = hostMap.get(providerAddress);
            if (providers == null) {
                hostMap.putIfAbsent(providerAddress, Sets.newHashSet());
                providers = hostMap.get(providerAddress);
            }
            String providerAkkaPath = providerAddress + "/user/" + pc.toPath();
            providers.add(providerAkkaPath);

            Set<String> paths = pathMap.get(pc.toPath());
            if (paths == null) {
                pathMap.putIfAbsent(pc.toPath(), Sets.newHashSet());
                paths = pathMap.get(pc.toPath());
            }
            paths.add(providerAkkaPath);

            referenceInbox.refreshPaths(pathMap);
        }
    }



    public static Props props(ReferenceInbox referenceInbox) {

        return Props.create(ReferenceActor.class, () -> new ReferenceActor(referenceInbox));
    }
}