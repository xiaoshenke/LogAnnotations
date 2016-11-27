package wuxian.me.logannotationsdemo.sub;

import android.util.Log;

import wuxian.me.logannotations.LOG;
import wuxian.me.logannotations.LogAll;
import wuxian.me.logannotations.NoLog;
import wuxian.me.logannotationsdemo.IWhatever;
import wuxian.me.logannotationsdemo.MainActivity;

/**
 * Created by wuxian on 23/11/2016.
 */


public class SubMainActivity extends MainActivity {

    @LOG
    protected void testD(Object... args) {
        Log.i("SubMainActivity", "in func testD ,powered by LOG Annotation");
        ;
    }

    @Override
    protected void testA() {

        Log.i("SubMainActivity", "in func testA ,powered by LOG Annotation");

    }

    public void say() {

    }

    @LOG
    private void testC() {

        Log.i("SubMainActivity", "in func testC ,powered by LOG Annotation");

        IWhatever whatever = new IWhatever() {
            @LOG
            @Override
            public void say() {

            }
        };
    }

    private class What implements IWhatever {
        @LOG
        @Override
        public void say() {

        }
    }

}
