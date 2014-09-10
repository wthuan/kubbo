package com.ifeng.kubbo.remote.benchmark.demo;

import scala.concurrent.Future;

/**
 * <title>Echo</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-10
 */
public interface Echo {


    public String syncEcho(String str);


    public Future<String> asyncEcho(String str);



}