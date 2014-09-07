package com.ifeng.kubbo.remote;

/**
 * <title>Reference</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-5
 */
public interface Reference {


    public <T> T getRef(Class<? super T> clazz, String group, String version);


}