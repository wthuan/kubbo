package com.ifeng.kubbo.akka;

import akka.actor.ActorSystem;
import com.typesafe.config.ConfigFactory;

import static com.ifeng.kubbo.akka.Constants.SEED_NODE_ROLE;

/**
 * <title>SeedNodeBootstrap</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-9
 */
public class SeedNodeBootstrap {

    public static void main(String[] args) {

        int port = 5002;
        if (args != null && args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        ActorSystem system = ActorSystem.create("kubbo", ConfigFactory.parseString("akka.cluster.roles=[" + SEED_NODE_ROLE + "]")
                .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port))
                .withFallback(ConfigFactory.load()));
        System.out.println("seed-nodes start");
        for(;;){
            synchronized (SeedNodeBootstrap.class){
                try {
                    SeedNodeBootstrap.class.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}