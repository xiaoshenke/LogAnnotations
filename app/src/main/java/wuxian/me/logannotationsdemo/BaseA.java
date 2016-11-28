package wuxian.me.logannotationsdemo;

import android.support.annotation.NonNull;
import android.util.Log;

import wuxian.me.logannotations.LOG;
import wuxian.me.logannotations.LogAll;
import wuxian.me.logannotations.NoLog;

/**
 * Created by wuxian on 23/11/2016.
 */

@LogAll
public abstract class BaseA {

    protected void call() {
        Log.i("BaseA", "in func call ,powered by LOG Annotation");
        ;
    }

    protected abstract void call(@NonNull final String name);
}
