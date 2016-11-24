package wuxian.me.logannotationsdemo;

import android.util.Log;


import wuxian.me.logannotations.LOG;
import wuxian.me.logannotations.NoLog;

/**
 * Created by wuxian on 23/11/2016.
 */

@NoLog
public class SubA extends A {

    @LOG(level = LOG.LEVEL_DEBUG)
    private void call(String name, String xing) {
    }

    @LOG(level = LOG.LEVEL_DEBUG)
    public void call(String name) {
        super.call(name);
    }
}
