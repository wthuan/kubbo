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
        if(message.equals(Protocol.SHAKE_HANDS_ALL)){
            ActorRef shaker = getSender();

        }
    }

    public static Props props(ReferenceInbox referenceInbox) {

        return Props.create(ReferenceActor.class, () -> new ReferenceActor(referenceInbox));
    }
}