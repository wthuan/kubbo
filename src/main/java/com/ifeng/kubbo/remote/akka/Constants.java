package com.ifeng.kubbo.remote.akka;

/**
 * <title>Constants</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-2
 */
public abstract class Constants {


    public static final int CPU_CORE = Runtime.getRuntime().availableProcessors();

    public static final String SYSTEM = "kubbo";


    public static final String PROVIDER_ROLE = "provider";


    public static final String CONSUMER_ROLE = "consumer";


    public static final String SEED_NODE_ROLE = "seed-nodes";

//    public static final String PROVIDER_ACTOR = "provider";

//    public static final String PROVIDER_ACTOR_PATH = "/user/provider";


    public static final String CONSUMER_ACTOR = "consumer";


    public static final String CONSUMER_ACTOR_PATH = "/user/consumer";


    public static final int TYPED_ACTOR_NUM = CPU_CORE;


    public static final long WAIT_PROVIDER_REGISTER_TIME =5000;

    public static final long WAIT_ADD_ROUTEE_TIME = 3000;


    public static final int PROVIDER_TOTAL_INSTANCE = 64;
}