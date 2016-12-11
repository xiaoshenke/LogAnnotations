package wuxian.me.logannotationsdemo;

import android.util.Log;
import wuxian.me.logannotations.LOG;
import wuxian.me.logannotations.LogAll;
import wuxian.me.logannotations.NoLog;
import wuxian.me.logannotationsdemo.sub.SubMainActivity;

/**
 * Created by wuxian on 23/11/2016.
 */


public class SubA<T extends MainActivity & IWhatever & IWhat> extends A {

    private void call(String name, String xing) {

        Log.i("SubA", "in func call ,powered by LOG Annotation");

    }

    public void call(String name) {
        super.call(name);
        Log.i("SubA", "in func call ,powered by LOG Annotation");
    }
}
