package wuxian.me.logannotationsdemo;

import wuxian.me.logannotations.Log;

/**
 * Created by wuxian on 23/11/2016.
 */

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