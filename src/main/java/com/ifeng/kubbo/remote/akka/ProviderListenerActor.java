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

import java.util.List;

import static com.ifeng.kubbo.remote.akka.Constants.CONSUMER_ACTOR_PATH;
import static com.ifeng.kubbo.remote.akka.Constants.CONSUMER_ROLE;


public class ProviderListenerActor extends UntypedActor{

    private Cluster cluster = Cluster.get(getContext().system());
    private ActorRef mediator = DistributedPubSubExtension.get(getContext().system()).mediator();
    private LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private ProviderContainer container;

    private ProviderListenerActor(ProviderContainer container) {
        this.container = container;
    }


    @Override
    public void preStart() throws Exception {

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
        }else if(message.equals(Protocol.SHAKE_HANDS_ALL)) {
            mediator.forward(new DistributedPubSubMediator.SendToAll(CONSUMER_ACTOR_PATH,Protocol.SHAKE_HANDS_ALL),getContext());
        }else{
            logger.warning("UnKnown message {}",message);
            unhandled(message);
        }
    }

    private void registerConsumer(Member member) {
        logger.info("sender register consumer command to {}", member.address());
        List<ActorRef> refs = container.list();
        //将provider 中的 actorRef 注册到consumer
        for(ActorRef ref: refs){
            getContext().actorSelection(member.address() + CONSUMER_ACTOR_PATH).tell(Protocol.SHAKE_HANDS_ALL,ref);
        }
    }

    private boolean isAliveConsumer(Member member) {
        return member.hasRole(CONSUMER_ROLE) && (member.status() == MemberStatus.up());
    }

    public static Props props(ProviderContainer container) {
        return Props.create(ProviderListenerActor.class, () -> new ProviderListenerActor(container));
    }
}
