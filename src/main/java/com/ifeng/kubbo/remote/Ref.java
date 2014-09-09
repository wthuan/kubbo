package com.ifeng.kubbo.remote;


import scala.concurrent.ExecutionContext;

public interface Ref {


    public <T> T getRef(Class<? super T> clazz, String group, String version);


    public ExecutionContext context();

}