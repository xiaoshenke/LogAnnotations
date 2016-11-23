package wuxian.me.logannotationsdemo.sub;

import wuxian.me.logannotations.Log;
import wuxian.me.logannotations.NoLog;
import wuxian.me.logannotationsdemo.IWhatever;
import wuxian.me.logannotationsdemo.MainActivity;

/**
 * Created by wuxian on 23/11/2016.
 */

@NoLog
public class SubMainActivity


        extends MainActivity implements IWhatever {

    @Log
    @Override
    protected void testA() {
        ;
    }

    @Log
    private void testC() {
        ;
    }
}
