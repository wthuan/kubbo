package com.ifeng.kubbo.remote.akka;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.contrib.pattern.DistributedPubSubExtension;
import akka.contrib.pattern.DistributedPubSubMediator;
import akka.dispatch.OnComplete;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.routing.*;
import com.google.common.collect.Lists;
import scala.concurrent.Future;

import java.util.HashMap;
import java.util.Map;

import static com.ifeng.kubbo.remote.akka.Constants.WAIT_ADD_ROUTEE_TIME;


public class ReferenceListenerActor extends UntypedActor {

    private ActorRef mediator = DistributedPubSubExtension.get(getContext().system()).mediator();
    private LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private RefInbox referenceInbox;

    private Map<String,ActorRef> routerMaps;
    private Map<ActorRef,Routee> routeeMap = new HashMap<>();
    public ReferenceListenerActor(RefInbox referenceInbox) {
        this.referenceInbox = referenceInbox;
        this.routerMaps = referenceInbox.getRouterMap();
    }


    @Override
    public void preStart() throws Exception {
        mediator.tell(new DistributedPubSubMediator.Put(getSelf()), getSelf());
        logger.info("ReferenceListener actor started");
    }

    @Override
    public void postStop() throws Exception {
        logger.info("ReferenceListener actor stopped !!!");
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if(message.equals(Protocol.SHAKE_HAND)) {
            register(getSender());
        }else if(message instanceof Terminated) {
            unRegister(((Terminated) message).getActor());
        }else{
            logger.error("Unknown message:{}",message);
        }
    }

    private void register(ActorRef shaker){

        logger.info("handler msg[register] from path={}",shaker.path());
        getContext().watch(shaker);

        String path = shaker.path().toStringWithoutAddress();


        ActorRef router = routerMaps.get(path);


        //create router,if not present or terminated
        if(router == null || router.isTerminated()) {
            logger.info("create consumer router|path={}",path);
            RoundRobinGroup group = new RoundRobinGroup(Lists.newArrayList());
            router = getContext().actorOf(group.props());
            routerMaps.put(path,router);
        }

        logger.info("add routee to consumer router|path={}",path);
        Routee newRoutee = new ActorRefRoutee(shaker);
        routeeMap.put(shaker, newRoutee);
        //tell is async,msg may not routed,so send getRoutee msg to notify path.intern().wait()
        router.tell(new AddRoutee(newRoutee),null);
        Future<Object> future = Patterns.ask(router, GetRoutees.getInstance(), WAIT_ADD_ROUTEE_TIME );
        future.onComplete(new OnComplete<Object>(){
            @Override
            public void onComplete(Throwable failure, Object success) throws Throwable {
                if(failure !=null){
                    logger.error(failure,"add Routee error");
                    return;
                }

                if(success == null){
                    logger.error("get routee is null");
                    return;
                }

                logger.info("addRoutee success,notify waitings");
                synchronized (path.intern()){
                    path.intern().notifyAll();
                }

            }
        },getContext().dispatcher());
    }



    private void unRegister(ActorRef shaker){
        logger.info("handler msg[unRegister] from path={}",shaker.path());
        getContext().unwatch(shaker);

        String path = shaker.path().toStringWithoutAddress();


        ActorRef router = routerMaps.get(path);
        if(router == null){
            logger.error("router is null while unRegister|path={}",path);
            return;
        }

        if(router.isTerminated()){
            //TODO router terminal
            System.out.println("router is terminal");
        }
        Routee routee = routeeMap.remove(shaker);
        if(routee == null){
            logger.error("routee is null while unRegister|shaker={}",shaker);
            return;
        }

        logger.info("remove provider|path={}",shaker.path().toStringWithAddress(shaker.path().address()));
        router.tell(new RemoveRoutee(routee),null);
//        Future<Object> future = Patterns.ask(router, GetRoutees.getInstance(), WAIT_ADD_ROUTEE_TIME );
//        future.onComplete(new OnComplete<Object>(){
//            @Override
//            public void onComplete(Throwable failure, Object success) throws Throwable {
//                failure.printStackTrace();
//                System.out.println(success);
//            }
//        },getContext().dispatcher());

    }



    public static Props props(RefInbox referenceInbox) {
        return Props.create(ReferenceListenerActor.class, () -> new ReferenceListenerActor(referenceInbox));
    }
}