package com.ifeng;


import scala.concurrent.Future;

/**
 * Created by jiangyou on 14-9-4.
 */
public interface TestService {
    public void testVoid(long sleep) throws Exception;

    public int testReturnInt(long sleep)throws Exception;

    public Future testReturnFuture(long sleep)throws Exception;


    public int testReturnIntWithParam(int param);

}
