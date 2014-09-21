#!/bin/sh
 for file in ../lib/*.jar;
    do
      CLASSPATH=$CLASSPATH:$file
    done



java -cp $CLASSPATH com.ifeng.kubbo.benchmark.Bootstrap $@