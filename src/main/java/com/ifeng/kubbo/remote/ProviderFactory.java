package com.ifeng.kubbo.remote;

/**
 * Created by jiangyou on 14-9-4.
 */
public interface ProviderFactory {

    public <T> T create(Class<T> clazz, T implement, String group, String version);
}
