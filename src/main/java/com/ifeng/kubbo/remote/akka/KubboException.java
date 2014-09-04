package com.ifeng.kubbo.remote.akka;

/**
 * <title>KubboException</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-4
 */
public class KubboException extends RuntimeException {

    public KubboException() {
        super();
    }

    public KubboException(String message) {
        super(message);
    }

    public KubboException(String message, Throwable cause) {
        super(message, cause);
    }

    public KubboException(Throwable cause) {
        super(cause);
    }


}