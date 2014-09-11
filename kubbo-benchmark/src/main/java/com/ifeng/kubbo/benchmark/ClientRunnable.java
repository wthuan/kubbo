package com.ifeng.kubbo.benchmark;


import java.util.Map;

public interface ClientRunnable extends Runnable{
    public Map<String,Object> getResults();
}