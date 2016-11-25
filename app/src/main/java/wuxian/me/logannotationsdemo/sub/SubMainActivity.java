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

    @LOG
    @Override
    protected void testA() {
        ;
    }

    @LOG
    private void testC() {
        ;
    }
}
