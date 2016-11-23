package wuxian.me.logannotationsdemo;

import wuxian.me.logannotations.LOG;

/**
 * Created by wuxian on 23/11/2016.
 */

public class SubA extends A {

    @LOG(level = 1)
    private void call(String name, String xing) {
        ;
    }

    @LOG(level = 1)
    public void call(String name) {
        super.call(name);
    }
}
