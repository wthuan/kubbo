package com.ifeng.kubbo.remote.akka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.contrib.pattern.DistributedPubSubExtension;
import akka.contrib.pattern.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class ReferenceListenerActor extends UntypedActor {

    private ActorRef mediator = DistributedPubSubExtension.get(getContext().system()).mediator();
    private LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private ReferenceInbox referenceInbox;

    private Map<String, Set<ActorRef>> shakerMaps = new HashMap<>();
    private Map<String,ActorRef> routerMaps = new HashMap<>();
    private Map<ActorRef,Routee> routeeMap = new HashMap<>();
    public ReferenceListenerActor(ReferenceInbox referenceInbox) {
        this.referenceInbox = referenceInbox;
    }



    @Override
    public void preStart() throws Exception {
        mediator.tell(new DistributedPubSubMediator.Put(getSelf()), getSelf());
    }

    @Override
    public void postStop() throws Exception {
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if(message.equals(Protocol.SHAKE_HANDS_ALL)) {
            register(getSender());
        }else if(message instanceof Terminated) {
            unRegister(((Terminated) message).actor());
        }else{
            logger.error("Unknown message:{}",message);
        }
    }

    private void register(ActorRef shaker){
        logger.info("handler msg[register] from path={}",shaker.path());
        String path = shaker.path().toStringWithoutAddress();
        Set<ActorRef> shakerSet = shakerMaps.get(path);
        if (shakerSet == null) {
            shakerSet = Sets.newHashSet();
            shakerMaps.put(path,shakerSet);
        }
        shakerSet.add(shaker);

        ActorRef router = routerMaps.get(path);
        //create router,if not present
        if(router == null) {
            RoundRobinGroup group = new RoundRobinGroup(Lists.newArrayList(shaker.path().toStringWithAddress(shaker.path().address())));
            router = getContext().actorOf(group.props());
            routerMaps.put(path,router);
        }else{
            Routee newRoutee = new ActorRefRoutee(shaker);
            routeeMap.put(shaker, newRoutee);
            router.tell(new AddRoutee(newRoutee),null);
            logger.info("send AddRoutee msg to router|path={}",router);
        }

        referenceInbox.refresh(routerMaps);
        getContext().watch(shaker);
    }



    private void unRegister(ActorRef shaker){
        logger.info("handler msg[unRegister] from path={}",shaker.path());
        String path = shaker.path().toStringWithoutAddress();
        Set<ActorRef> shakerSet = shakerMaps.get(path);
        if (shakerSet != null) {
            shakerSet.remove(shaker);
            Routee routee = routeeMap.get(shaker);
            ActorRef router = routerMaps.get(path);
            router.tell(new RemoveRoutee(routee),null);
            logger.info("send removeRoutee msg to router|path={}",router);
        }
        getContext().unwatch(shaker);
    }

    public static Props props(ReferenceInbox referenceInbox) {

        return Props.create(ReferenceListenerActor.class, () -> new ReferenceListenerActor(referenceInbox));
    }
}