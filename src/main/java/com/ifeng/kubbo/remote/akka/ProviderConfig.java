
package com.ifeng.kubbo.remote.akka;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Objects;

/**
 * <title>ServiceInfo</title>
 * <p></p>
 * Copyright © 2013 Phoenix New Media Limited All Rights Reserved.
 *
 * @author zhuwei
 *         14-9-1
 */
public class ProviderConfig implements Serializable {
    private transient Class<?> clazz;
    private String klass;
    private String group;
    private String version;
    private transient Object implement;
    public ProviderConfig(Class<?> clazz, Object implement, String group, String version) {
        Objects.requireNonNull(clazz, "provider clazz required non null");

        this.clazz = clazz;
        this.klass = clazz.getCanonicalName();
        this.group = StringUtils.defaultIfBlank(group,"default");
        this.version = StringUtils.defaultIfBlank(version,"default");
        this.implement = implement;
    }

    public ProviderConfig(Class<?> clazz,String group,String version){
        this(clazz,null,group,version);
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getKlass() {
        return klass;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    public Object getImplement() {
        return implement;
    }

    public String toPath() {
        StringBuilder sb = new StringBuilder();
        sb.append(klass);
        return sb.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProviderConfig that = (ProviderConfig) o;

        if (!group.equals(that.group)) return false;
        if (!klass.equals(that.klass)) return false;
        if (!version.equals(that.version)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = klass.hashCode();
        result = 31 * result + group.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return this.toPath();
    }
}
