package com.ifeng.kubbo.remote.akka;

import junit.framework.TestCase;
import org.junit.Test;

/**
 * <title>ReferenceInbox</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-2
 */
public class ReferenceInboxTest extends TestCase {


    @Test
    public void testCreate() throws InterruptedException {
        ReferenceInbox referenceInbox = ReferenceInbox.get("akka.tcp://kubbo@127.0.0.1:1111", 2222);
        Thread.sleep(5000);
        ProviderManagerInboxTest.Test test = referenceInbox.getReference(ProviderManagerInboxTest.Test.class, "", "");
        int size =100000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            test.test();
        }

        long end = System.currentTimeMillis();
        System.out.println(size*1000/(end-start));
        Thread.sleep(10000000);
    }
}