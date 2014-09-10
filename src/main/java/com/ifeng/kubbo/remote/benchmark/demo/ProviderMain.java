package com.ifeng.kubbo.remote.benchmark.demo;

import com.ifeng.kubbo.remote.ProviderLifeCycle;
import com.ifeng.kubbo.remote.akka.ProviderContainer;

/**
 * <title>ProviderMain</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-10
 */
public class ProviderMain {


    public static void main(String[] args) throws InterruptedException {

        ProviderLifeCycle container = new ProviderContainer();
        container.start(Echo.class, new EchoImpl(), null, null);

        System.out.println("Provider start...");
        Thread.sleep(Integer.MAX_VALUE);

    }
}