package wuxian.me.logannotationsdemo;

import wuxian.me.logannotations.Log;
import wuxian.me.logannotations.NoLog;

/**
 * Created by wuxian on 23/11/2016.
 */

@NoLog
public class A extends BaseA {

    @Log
    protected void call() {
        ;
    }
}
