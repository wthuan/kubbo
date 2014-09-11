package com.ifeng.kubbo.benchmark.demo;

import akka.dispatch.Futures;
import scala.concurrent.Future;

import static com.ifeng.kubbo.akka.Context.context;


/**
 * <title>EchoImpl</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-10
 */
public class EchoImpl implements Echo{


    @Override
    public String syncEcho(String str) {

        return str;
    }

    @Override
    public Future<String> asyncEcho(String str) {

        return Futures.future(() -> str, context());
    }
}