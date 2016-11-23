package wuxian.me.logannotationsdemo;

import wuxian.me.logannotations.Log;
import wuxian.me.logannotations.NoLog;

/**
 * Created by wuxian on 23/11/2016.
 */

@NoLog
public class BaseA {

    @Log
    protected void call() {
        ;
    }

    @Log
    protected void call(String name) {
        ;
    }
}
