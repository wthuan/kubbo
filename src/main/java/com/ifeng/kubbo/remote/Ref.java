package com.ifeng.kubbo.remote;


public interface Ref {


    public <T> T getRef(Class<? super T> clazz, String group, String version);

}