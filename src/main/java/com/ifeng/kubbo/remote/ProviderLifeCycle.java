package com.ifeng.kubbo.remote;

/**
 * Created by jiangyou on 14-9-4.
 */
public interface ProviderLifeCycle {


    /**
     * 创建provider
     * @param clazz
     * @param implement
     * @param group
     * @param version
     * @param <T>
     * @return
     */
    public <T> T start(Class<T> clazz, T implement, String group, String version);


    /**
     * 停止provider
     * @param clazz
     * @param group
     * @param version
     */
    public void stop(Class<?> clazz,String group,String version);


    /**
     * 添加增加actor
     * @param clazz
     * @param group
     * @param version
     * @param <T>
     * @return
     */
    public <T> T increaseActor(Class<T> clazz,String group,String version);


    /**
     * 减少actor
     * @param clazz
     * @param group
     * @param version
     * @param <T>
     * @return
     */
    public <T> T decreaseActor(Class<T> clazz,String group,String version);


}
