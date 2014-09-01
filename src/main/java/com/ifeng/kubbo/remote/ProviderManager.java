package com.ifeng.kubbo.remote;

/**
 * Created by jiangyou on 14-9-1.
 */
public interface ProviderManager {



    public void publish(Class<?> clazz, Object implement, String group, String version);



    public void remove(Class<?>clazz,String group,String version);


}




