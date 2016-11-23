package wuxian.me.logannotationsdemo;

import wuxian.me.logannotations.LOG;
import wuxian.me.logannotations.NoLog;

/**
 * Created by wuxian on 23/11/2016.
 */

@NoLog
public class A extends BaseA {

    @LOG
    protected void call() {
        ;
    }
}
