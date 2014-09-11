package com.ifeng.kubbo.akka;


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