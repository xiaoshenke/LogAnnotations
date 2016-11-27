package wuxian.me.logannotationsdemo;

import android.util.Log;

import wuxian.me.logannotations.LOG;
import wuxian.me.logannotations.NoLog;

/**
 * Created by wuxian on 23/11/2016.
 */


public class A extends BaseA {


    protected void call() {
        Log.i("A", "in func call ,powered by LOG Annotation");
        ;
    }

    protected void callA() {
        Log.i("A", "in func callA ,powered by LOG Annotation");
    }
}
