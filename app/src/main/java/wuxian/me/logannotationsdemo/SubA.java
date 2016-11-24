package wuxian.me.logannotationsdemo;

import android.util.Log;


import wuxian.me.logannotations.LOG;

/**
 * Created by wuxian on 23/11/2016.
 */

public class SubA extends A {

    @LOG(level = LOG.LEVEL_DEBUG)
    private void call(String name, String xing) {
    }

    @LOG(level = LOG.LEVEL_DEBUG)
    public void call(String name) {
        super.call(name);
    }
}
