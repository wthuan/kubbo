package com.ifeng.kubbo.benchmark;

import com.ifeng.kubbo.akka.SeedNodeBootstrap;
import com.ifeng.kubbo.benchmark.demo.ProviderMain;



public class Bootstrap {


    public static void main(String[] args) throws Exception {

        System.out.println("java -jar [seedNode,provider,benchmark]");
        if(args.length == 0 || "benchmark".equals(args[0])){
            RpcBenchmarkClient.main(null);
        }else if("seedNode".equals(args[0])){
            System.out.println("start SeedNode");
            SeedNodeBootstrap.main(null);
        }else if("provider".equals(args[0])) {
            System.out.println("start Provider");
            ProviderMain.main(null);
        }else{
            throw new IllegalArgumentException();
        }
    }
}