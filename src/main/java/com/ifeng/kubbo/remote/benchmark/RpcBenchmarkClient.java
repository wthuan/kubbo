package com.ifeng.kubbo.remote.benchmark;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * <title>RpcBenchmarkClient</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-10
 */
public class RpcBenchmarkClient extends AbstractBenchmarkClient{

    @Override
    public ClientRunnable getClientRunnable(CyclicBarrier barrier, CountDownLatch latch,int requestNum) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        String className = properties.getProperty("classname");
        Class[] parameterTypes = new Class[] { CyclicBarrier.class,
                CountDownLatch.class, int.class };
        Object[] parameters = new Object[] {barrier, latch,
                requestNum};
        return (ClientRunnable) Class.forName(className).getConstructor(parameterTypes).newInstance(parameters);
    }

    public static void main(String[] args) throws Exception {
        new RpcBenchmarkClient().run(args);
    }
}