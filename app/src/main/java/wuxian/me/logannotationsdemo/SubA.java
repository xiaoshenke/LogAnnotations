package wuxian.me.logannotationsdemo;

import android.util.Log;


import wuxian.me.logannotations.LOG;
import wuxian.me.logannotations.LogAll;
import wuxian.me.logannotations.NoLog;

/**
 * Created by wuxian on 23/11/2016.
 */

@LogAll
public class SubA extends A {

    private void call(String name, String xing) {
        Log.i("SubA", "in func call ,powered by LOG Annotation");
        ;
    }

    @LOG(level = LOG.LEVEL_DEBUG)
    public void call(String name) {
        super.call(name);
        Log.d("SubA", "in func call ,powered by LOG Annotation");
    }
}
