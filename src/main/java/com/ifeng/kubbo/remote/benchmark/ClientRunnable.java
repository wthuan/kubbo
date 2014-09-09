package com.ifeng.kubbo.remote.benchmark;


import java.util.List;

public interface ClientRunnable extends Runnable{
    public List<long[]> getResults();
}