package com.ifeng.kubbo;


public interface Ref {


    public <T> T getRef(Class<? super T> clazz, String group, String version);

}