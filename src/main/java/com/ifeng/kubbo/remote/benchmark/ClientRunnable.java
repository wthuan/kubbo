package com.ifeng.kubbo.remote.benchmark;


import java.util.Map;

public interface ClientRunnable extends Runnable{
    public Map<String,Object> getResults();
}