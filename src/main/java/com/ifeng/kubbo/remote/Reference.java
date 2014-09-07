package com.ifeng.kubbo.remote;


public interface Reference {


    public <T> T getRef(Class<? super T> clazz, String group, String version);


}