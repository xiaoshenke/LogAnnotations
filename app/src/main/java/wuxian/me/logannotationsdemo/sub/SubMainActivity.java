package wuxian.me.logannotationsdemo.sub;

import android.util.Log;

import wuxian.me.logannotations.LOG;
import wuxian.me.logannotations.NoLog;
import wuxian.me.logannotationsdemo.IWhatever;
import wuxian.me.logannotationsdemo.MainActivity;

/**
 * Created by wuxian on 23/11/2016.
 */


public class SubMainActivity extends MainActivity implements IWhatever {

    @Override
    protected void testA() {
        ;
    }

    @LOG
    private void testC() {
        Log.i("SubMainActivity", "in func testC ,powered by LOG Annotation");

        IWhatever whatever = new IWhatever() {
            @LOG
            @Override
            public void say() {

                Log.i("SubMainActivity", "in func say ,powered by LOG Annotation");

            }
        };
    }

    @LOG
    @Override
    public void say() {
        Log.i("SubMainActivity", "in func say ,powered by LOG Annotation");
    }

    private class What implements IWhatever {

        @LOG
        @Override
        public void say() {

        }
    }
}
