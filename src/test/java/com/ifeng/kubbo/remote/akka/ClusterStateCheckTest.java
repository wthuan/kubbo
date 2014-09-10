package com.ifeng.kubbo.remote.akka;

import com.ifeng.TestService;
import com.ifeng.TestServiceImpl;
import com.ifeng.kubbo.remote.Ref;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * <title>ClusterStateCheckTest</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-8
 */
public class ClusterStateCheckTest  extends TestCase {


    public static void testStartProvider(int port){
        ProviderContainer container = new ProviderContainer(10);
        container.start(TestService.class,new TestServiceImpl(),null,null);

        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testStartProvider1(){
        testStartProvider(1111);
    }




    @Test
    public void testStartProvider2(){
        testStartProvider(2222);
    }


    @Test
    public void testStartProvider3(){
        testStartProvider(3333);
    }



    @Test
    public static void testStartConsumer() throws InterruptedException {

        Ref ref = Reference.get();

        TestService service = ref.getRef(TestService.class,null,null);
//
//        RefInbox referenceInbox = RefInbox.get("akka.tcp://kubbo@127.0.0.1:1111", 4444);
//        TestService service = referenceInbox.getRef(TestService.class, null, null);
        for(int i=0;i<10000;i++) {
            System.out.println(i);
            try{

                service.testReturnIntWithParam(i);
            }catch(Exception e){
                System.out.println(e);
            }

            Thread.sleep(1000);
        }
        Thread.sleep(100000000);
    }
}