package wuxian.me.logannotationsdemo;

import android.util.Log;

import wuxian.me.logannotations.LOG;
import wuxian.me.logannotations.LogAll;
import wuxian.me.logannotations.NoLog;

/**
 * Created by wuxian on 23/11/2016.
 */

@LogAll
public class BaseA {

    protected void call() {
        Log.i("BaseA", "in func call ,powered by LOG Annotation");
        ;
    }

    protected void call(final String name) {
        ;
    }
}
