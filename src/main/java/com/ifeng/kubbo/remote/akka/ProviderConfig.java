
package com.ifeng.kubbo.remote.akka;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Objects;


public class ProviderConfig implements Serializable {
    private transient Class<?> clazz;
    private String className;
    private String group;
    private String version;
    private transient Object implement;
    public ProviderConfig(Class<?> clazz, Object implement, String group, String version) {
        Objects.requireNonNull(clazz, "provider clazz required non null");

        this.clazz = clazz;
        this.className = clazz.getCanonicalName();
        this.group = StringUtils.defaultIfBlank(group,"default");
        this.version = StringUtils.defaultIfBlank(version,"default");
        this.implement = implement;
    }

    public ProviderConfig(Class<?> clazz,String group,String version){
        this(clazz,null,group,version);
    }

    public String getClassName() {
        return className;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public Object getImplement() {
        return implement;
    }

    public String toPath() {
        StringBuilder sb = new StringBuilder();
        sb.append(className);
        return sb.toString();
    }

    public String toAkkaPath(){
        return "/user/"+toPath();
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProviderConfig that = (ProviderConfig) o;

        if (!group.equals(that.group)) return false;
        if (!version.equals(that.version)) return false;
        if (!className.equals(that.className)) return false;


        return true;
    }

    @Override
    public int hashCode() {
        int result = className.hashCode();
        result = 31 * result + group.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return this.toPath();
    }
}
