package com.ifeng.kubbo.remote.akka;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * <title>AkkServiceManagerTest</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-1
 */
public class ProviderManagerInboxTest extends TestCase {


    public static interface Test{
        public void test();
    }

    public static class TestImpl implements Test{

        @Override
        public void test() {
            System.out.println(Thread.currentThread().getName() + " execute");
        }
    }

    @org.junit.Test
    public void testCreate() throws InterruptedException {
        Set<ProviderConfig> providers = new HashSet<>();
        providers.add(new ProviderConfig(Test.class, new TestImpl(), "test", "1.0.0"));
        ProviderManagerInbox.get(providers, "akka.tcp://kubbo@127.0.0.1:1111", 1111);
        Thread.sleep(5000);
    }
}