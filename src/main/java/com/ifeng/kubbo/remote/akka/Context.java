package com.ifeng.kubbo.remote.akka;

import scala.concurrent.ExecutionContext;

/**
 * <title>Context</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-10
 */
public class Context {


    private static final ExecutionContext CTX = Reference.get().context();


    public static ExecutionContext context() {
        return CTX;
    }
}